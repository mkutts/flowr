package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: UserProfile? = null,
    val reviewCount: Int = 0,
    val products: List<Product> = emptyList()
)

class PublicProfileViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // not strictly required, but ensures user is authed

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState

    private val targetUid: String = savedStateHandle.get<String>("uid").orEmpty()

    init { refresh() }

    fun refresh() {
        if (targetUid.isBlank()) {
            _uiState.value = PublicProfileUiState(isLoading = false, error = "Missing user id")
            return
        }
        if (auth.currentUser == null) {
            _uiState.value = PublicProfileUiState(isLoading = false, error = "Not signed in")
            return
        }

        _uiState.value = PublicProfileUiState(isLoading = true)

        viewModelScope.launch {
            try {
                // 1) Load profile
                val profileSnap = db.collection("users").document(targetUid).get().awaitk()
                val profile = profileSnap.toObject(UserProfile::class.java)?.copy(uid = targetUid)

                // 2) Load this user's reviews to compute count + productIds
                val reviewsSnap = db.collection("reviews")
                    .whereEqualTo("userId", targetUid)
                    .limit(300)
                    .get()
                    .awaitk()

                val productIds = reviewsSnap.documents.mapNotNull { it.getString("productId") }.distinct()
                val count = reviewsSnap.size()

                // 3) Fetch products they reviewed (in chunks of 10 for whereIn)
                val products = mutableListOf<Product>()
                for (chunk in productIds.chunked(10)) {
                    val qs = db.collection("products")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .awaitk()
                    products += qs.documents.mapNotNull { d ->
                        d.toObject(Product::class.java)?.copy(id = d.id)
                    }
                }

                _uiState.value = PublicProfileUiState(
                    isLoading = false,
                    profile = profile,
                    reviewCount = count,
                    products = products
                )
            } catch (e: Exception) {
                _uiState.value = PublicProfileUiState(isLoading = false, error = e.message ?: "Failed to load profile")
            }
        }
    }
}
