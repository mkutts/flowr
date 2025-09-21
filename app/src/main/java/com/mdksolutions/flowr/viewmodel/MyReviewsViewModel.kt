package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MyReviewsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val products: List<Product> = emptyList()
)

class MyReviewsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MyReviewsUiState())
    val uiState: StateFlow<MyReviewsUiState> = _uiState

    init { refresh() }

    fun refresh() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = MyReviewsUiState(isLoading = false, error = "Not signed in")
            return
        }

        _uiState.value = MyReviewsUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val uid = user.uid

                // 1) Get this user's reviews from the top-level "reviews" collection
                val reviewsSnap = db.collection("reviews")
                    .whereEqualTo("userId", uid)
                    .limit(200) // adjust if you expect more
                    .get()
                    .awaitk()

                // 2) Collect unique product IDs
                val productIds = reviewsSnap.documents
                    .mapNotNull { it.getString("productId") }
                    .distinct()

                if (productIds.isEmpty()) {
                    _uiState.value = MyReviewsUiState(isLoading = false, products = emptyList())
                    return@launch
                }

                // 3) Fetch products in chunks of 10 (whereIn limit)
                val chunks = productIds.chunked(10)
                val products = mutableListOf<Product>()
                for (chunk in chunks) {
                    val qs = db.collection("products")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .awaitk()
                    products += qs.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }
                }

                _uiState.value = MyReviewsUiState(
                    isLoading = false,
                    products = products
                )
            } catch (e: Exception) {
                _uiState.value = MyReviewsUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load reviews"
                )
            }
        }
    }
}
