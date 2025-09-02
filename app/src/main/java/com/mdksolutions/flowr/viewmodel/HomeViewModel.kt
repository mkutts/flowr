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

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                val success = repo.addProduct(product)
                _baseState.update {
                    it.copy(
                        errorMessage = if (success) "Product added successfully" else "Failed to add product"
                    )
                }
            } catch (e: Exception) {
                _baseState.update { it.copy(errorMessage = "Error: ${e.message}") }
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
