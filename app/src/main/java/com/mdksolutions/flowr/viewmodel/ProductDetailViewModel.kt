package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject   // ✅ KTX mapper
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.model.Review
import com.mdksolutions.flowr.repository.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoadingProduct: Boolean = false,
    val isLoadingReviews: Boolean = false,
    val isSubmittingReview: Boolean = false, // ✅ NEW
    val reviewAdded: Boolean = false,        // ✅ NEW
    val product: Product? = null,
    val reviews: List<Review> = emptyList(),
    val errorMessage: String? = null
)

class ProductDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val reviewRepo = ReviewRepository()

    private val _uiState = MutableStateFlow(ProductDetailUiState(isLoadingProduct = true))
    val uiState: StateFlow<ProductDetailUiState> get() = _uiState

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProduct = true)
            try {
                db.collection("products").document(productId).get()
                    .addOnSuccessListener { document ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingProduct = false,
                            // ✅ KTX generic mapper + keep Firestore document ID
                            product = document.toObject<Product>()?.copy(id = document.id)
                        )
                    }
                    .addOnFailureListener {
                        _uiState.value = _uiState.value.copy(
                            isLoadingProduct = false,
                            errorMessage = "Failed to load product"
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProduct = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun loadReviews(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReviews = true)
            try {
                reviewRepo.getReviews(productId).collect { reviewList ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingReviews = false,
                        reviews = reviewList
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingReviews = false,
                    errorMessage = "Failed to load reviews"
                )
            }
        }
    }

    fun addReview(review: Review) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingReview = true, reviewAdded = false)
            try {
                val success = reviewRepo.addReview(review)
                if (success) {
                    updateProductAggregates(review.productId, review.feels, review.activity)
                    _uiState.value = _uiState.value.copy(
                        isSubmittingReview = false,
                        reviewAdded = true,
                        errorMessage = "Review added successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingReview = false,
                        errorMessage = "Failed to add review"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingReview = false,
                    errorMessage = "Error adding review: ${e.message}"
                )
            }
        }
    }

    private fun updateProductAggregates(productId: String, feels: List<String>, activity: String) {
        val productRef = db.collection("products").document(productId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(productRef)
            if (snapshot.exists()) {
                val currentFeels = snapshot.get("topFeels") as? List<String> ?: emptyList()
                val currentActivities = snapshot.get("topActivities") as? List<String> ?: emptyList()

                val updatedFeels = (currentFeels + feels).groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .take(5)

                val updatedActivities = (currentActivities + activity).groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .take(5)

                transaction.update(productRef, mapOf(
                    "topFeels" to updatedFeels,
                    "topActivities" to updatedActivities
                ))
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearReviewAdded() {
        _uiState.value = _uiState.value.copy(reviewAdded = false)
    }
}
