package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject   // ✅ current extension
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.model.Review
import com.mdksolutions.flowr.repository.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoadingProduct: Boolean = false,
    val isLoadingReviews: Boolean = false,
    val isSubmittingReview: Boolean = false, // ✅ NEW
    val reviewAdded: Boolean = false,        // ✅ NEW
    val product: Product? = null,
    val reviews: List<Review> = emptyList(),
    val errorMessage: String? = null,

    // ✅ PATCH: fields for editing an existing review
    val editingReviewId: String? = null,
    val editedText: String = "",
    val editedRating: Float = 0f,
    val isSavingEdit: Boolean = false,
    val isDeletingReview: Boolean = false
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
            } catch (_: Exception) {
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
                val currentFeels = (snapshot.get("topFeels") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val currentActivities = (snapshot.get("topActivities") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

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

    // ─────────────────────────────────────────────────────────────
    // ✅ PATCH: Editing workflow
    // ─────────────────────────────────────────────────────────────

    /** Begin editing a specific review */
    fun startEditingReview(reviewId: String, currentText: String, currentRating: Double) {
        _uiState.update {
            it.copy(
                editingReviewId = reviewId,
                editedText = currentText,
                editedRating = currentRating.toFloat(),
                errorMessage = null
            )
        }
    }

    /** Update the draft text while editing */
    fun setEditedText(text: String) {
        _uiState.update { it.copy(editedText = text) }
    }

    /** Update the draft rating while editing */
    fun setEditedRating(rating: Float) {
        _uiState.update { it.copy(editedRating = rating) }
    }

    /** Cancel editing and clear draft */
    fun cancelEditingReview() {
        _uiState.update {
            it.copy(
                editingReviewId = null,
                editedText = "",
                editedRating = 0f,
                isSavingEdit = false
            )
        }
    }

    /**
     * Save the review edit.
     * Assumes reviews are stored under: /products/{productId}/reviews/{reviewId}
     * (If you also keep top-level /reviews, mirror this call as needed.)
     */
    fun updateReview(
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val state = _uiState.value
        val productId = state.product?.id
        val reviewId = state.editingReviewId
        val newText = state.editedText
        val newRating = state.editedRating.coerceIn(1f, 5f) // clamp to match rules

        if (productId.isNullOrBlank() || reviewId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Invalid product or review to update.") }
            return
        }

        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (authUid == null) {
            _uiState.update { it.copy(errorMessage = "Not signed in.") }
            return
        }

        val subRef = db.collection("products").document(productId)
            .collection("reviews").document(reviewId)
        val topRef = db.collection("reviews").document(reviewId)

        _uiState.update { it.copy(isSavingEdit = true, errorMessage = null) }

        // Try the subcollection path first
        subRef.get()
            .addOnSuccessListener { subSnap ->
                if (subSnap.exists()) {
                    // verify ownership
                    val ownerId = subSnap.getString("userId")
                    if (ownerId != authUid) {
                        _uiState.update { it.copy(isSavingEdit = false, errorMessage = "You can only edit your own review.") }
                        return@addOnSuccessListener
                    }
                    val updateMap = mapOf(
                        "reviewText" to newText,
                        "rating" to newRating,
                        "editedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    subRef.update(updateMap)
                        .addOnSuccessListener {
                            loadReviews(productId)
                            _uiState.update {
                                it.copy(
                                    isSavingEdit = false,
                                    editingReviewId = null,
                                    editedText = "",
                                    editedRating = 0f,
                                    errorMessage = null
                                )
                            }
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            _uiState.update { it.copy(isSavingEdit = false, errorMessage = e.message) }
                            onError(e)
                        }
                } else {
                    // Fallback to top-level /reviews/{reviewId}
                    topRef.get()
                        .addOnSuccessListener { topSnap ->
                            if (!topSnap.exists()) {
                                _uiState.update { it.copy(isSavingEdit = false, errorMessage = "Review not found.") }
                                return@addOnSuccessListener
                            }

                            val ownerId = topSnap.getString("userId")
                            if (ownerId != authUid) {
                                _uiState.update { it.copy(isSavingEdit = false, errorMessage = "You can only edit your own review.") }
                                return@addOnSuccessListener
                            }

                            val updateMap = mapOf(
                                "reviewText" to newText,
                                "rating" to newRating,
                                "editedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                            topRef.update(updateMap)
                                .addOnSuccessListener {
                                    loadReviews(productId)
                                    _uiState.update {
                                        it.copy(
                                            isSavingEdit = false,
                                            editingReviewId = null,
                                            editedText = "",
                                            editedRating = 0f,
                                            errorMessage = null
                                        )
                                    }
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    _uiState.update { it.copy(isSavingEdit = false, errorMessage = e.message) }
                                    onError(e)
                                }
                        }
                        .addOnFailureListener { e ->
                            _uiState.update { it.copy(isSavingEdit = false, errorMessage = e.message) }
                            onError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isSavingEdit = false, errorMessage = e.message) }
                onError(e)
            }
    }

    fun deleteReview(
        reviewId: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val state = _uiState.value
        val productId = state.product?.id
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        if (reviewId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Invalid review to delete.") }
            return
        }
        if (authUid == null) {
            _uiState.update { it.copy(errorMessage = "Not signed in.") }
            return
        }

        val subRef = if (!productId.isNullOrBlank()) {
            db.collection("products").document(productId)
                .collection("reviews").document(reviewId)
        } else null
        val topRef = db.collection("reviews").document(reviewId)

        _uiState.update { it.copy(isDeletingReview = true, errorMessage = null) }

        // helper to perform delete on a found/owned doc
        fun deleteOwned(ref: com.google.firebase.firestore.DocumentReference) {
            ref.delete()
                .addOnSuccessListener {
                    // Refresh list if we're on product detail
                    productId?.let { loadReviews(it) }
                    _uiState.update { it.copy(isDeletingReview = false, errorMessage = "Review deleted") }
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(isDeletingReview = false, errorMessage = e.message) }
                    onError(e)
                }
        }

        // Try subcollection first (if we have a productId), then top-level
        val tryTop: () -> Unit = {
            topRef.get()
                .addOnSuccessListener { snap ->
                    if (!snap.exists()) {
                        _uiState.update { it.copy(isDeletingReview = false, errorMessage = "Review not found.") }
                        return@addOnSuccessListener
                    }
                    val ownerId = snap.getString("userId")
                    if (ownerId != authUid) {
                        _uiState.update { it.copy(isDeletingReview = false, errorMessage = "You can only delete your own review.") }
                        return@addOnSuccessListener
                    }
                    deleteOwned(topRef)
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(isDeletingReview = false, errorMessage = e.message) }
                    onError(e)
                }
        }

        if (subRef == null) {
            tryTop()
        } else {
            subRef.get()
                .addOnSuccessListener { subSnap ->
                    if (subSnap.exists()) {
                        val ownerId = subSnap.getString("userId")
                        if (ownerId != authUid) {
                            _uiState.update { it.copy(isDeletingReview = false, errorMessage = "You can only delete your own review.") }
                            return@addOnSuccessListener
                        }
                        deleteOwned(subRef)
                    } else {
                        tryTop()
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(isDeletingReview = false, errorMessage = e.message) }
                    onError(e)
                }
        }
    }

}
