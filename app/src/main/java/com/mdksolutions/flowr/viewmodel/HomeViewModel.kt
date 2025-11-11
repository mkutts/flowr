package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// NEW: imports for unique-write path
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.google.firebase.firestore.FieldValue

class HomeViewModel : ViewModel() {

    private val repo = FirestoreRepository()

    // 🔁 Firestore listener is active only while someone collects it
    private val products: StateFlow<List<Product>> =
        repo.getProducts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    // Internal UI state for filters / loading / messages (without products list)
    private val _baseState = MutableStateFlow(HomeUiState(isLoading = true))

    // ✅ Public UI state combines base state + products stream
    val uiState: StateFlow<HomeUiState> =
        combine(_baseState, products) { state, items ->
            state.copy(products = items, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState(isLoading = true)
        )

    // --- Actions ---

    // NEW: normalized key builder for uniqueness (brand + name), unicode-safe
    private fun normalizeKeyPart(s: String): String =
        s.trim()
            .lowercase(Locale.US)
            .replace("[^\\p{L}\\p{N}]+".toRegex(), "-")
            .trim('-')

    // REPLACE the function:
    fun addProductUnique(product: Product) {
        viewModelScope.launch {
            try {
                val brandKey = normalizeKeyPart(product.brand)
                val nameKey  = normalizeKeyPart(product.name)
                val key = "${brandKey}__${nameKey}"

                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("products").document(key)

                // canonical payload for brand+name doc
                val data = hashMapOf(
                    "name" to product.name,
                    "brand" to product.brand,
                    "category" to product.category,
                    "strainType" to product.strainType,
                    "uniqueKey" to key,
                    "nameKey" to nameKey,
                    "brandKey" to brandKey,
                    // legacy single-state for back-compat (keep it; optional to remove later)
                    "state" to product.state,
                    // NEW multi-state list
                    "states" to listOf(product.state),
                    // keep your extra optional fields
                    "potencyUsesMg" to product.potencyUsesMg,
                    "thcPercent" to product.thcPercent,
                    "dosageMg" to product.dosageMg
                )

                db.runTransaction { txn ->
                    val snap = txn.get(docRef)
                    if (!snap.exists()) {
                        // create new doc
                        txn.set(docRef, data)
                        return@runTransaction null
                    }

                    // Doc exists → only merge state if "same product" by category+strainType
                    val existingCategory = snap.getString("category") ?: ""
                    val existingStrain   = snap.getString("strainType") ?: ""

                    // normalize for tolerant compare
                    fun norm(x: String) = x.trim().lowercase(Locale.US)

                    if (norm(existingCategory) == norm(product.category)
                        && norm(existingStrain) == norm(product.strainType)
                    ) {
                        // merge state atomically
                        txn.update(
                            docRef,
                            mapOf(
                                "states" to FieldValue.arrayUnion(product.state),
                                // keep legacy 'state' aligned to the first/most recent; harmless
                                "state" to product.state
                            )
                        )
                        return@runTransaction null
                    } else {
                        // same brand+name but different category/strain type → treat as conflict
                        throw FirebaseFirestoreException(
                            "Conflicting product metadata for existing brand+name (category/strainType differs).",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                }.await()

                _baseState.update { it.copy(errorMessage = "Product saved") }
            } catch (e: Exception) {
                val msg =
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED)
                        e.message ?: "Duplicate with conflicting fields."
                    else
                        "Error: ${e.message ?: "unknown error"}"
                _baseState.update { it.copy(errorMessage = msg) }
            }
        }
    }



    // ✅ Filters
    fun updateSearchQuery(query: String) {
        _baseState.update { it.copy(searchQuery = query) }
    }

    fun updateCategory(category: String?) {
        _baseState.update { it.copy(selectedCategory = category) }
    }

    fun updateState(state: String?) {
        _baseState.update { it.copy(selectedState = state) }
    }

    fun updateFeel(feel: String?) {
        _baseState.update { it.copy(selectedFeel = feel) }
    }

    fun updateActivity(activity: String?) {
        _baseState.update { it.copy(selectedActivity = activity) }
    }

    fun clearMessage() {
        _baseState.update { it.copy(errorMessage = null) }
    }
}
