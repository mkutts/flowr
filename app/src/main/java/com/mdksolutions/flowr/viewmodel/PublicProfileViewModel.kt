package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
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
    val products: List<Product> = emptyList(),
    val isSelf: Boolean = false,         // NEW
    val isFollowing: Boolean = false,    // NEW
    val isBusy: Boolean = false          // NEW
)

class PublicProfileViewModel(
    savedStateHandle: SavedStateHandle   // keep as param (not property) to avoid lint warning
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState

    private val targetUid: String = savedStateHandle.get<String>("uid").orEmpty()

    init { refresh() }

    fun refresh() {
        if (targetUid.isBlank()) {
            _uiState.value = PublicProfileUiState(isLoading = false, error = "Missing user id")
            return
        }
        val me = auth.currentUser?.uid
        if (me == null) {
            _uiState.value = PublicProfileUiState(isLoading = false, error = "Not signed in")
            return
        }

        _uiState.value = PublicProfileUiState(isLoading = true)

        viewModelScope.launch {
            try {
                // 1) Load target profile
                val profileSnap = db.collection("users").document(targetUid).get().awaitk()
                val profile = profileSnap.toObject(UserProfile::class.java)?.copy(uid = targetUid)

                // 2) Load target's reviews → count + productIds
                val reviewsSnap = db.collection("reviews")
                    .whereEqualTo("userId", targetUid)
                    .limit(300)
                    .get()
                    .awaitk()
                val productIds = reviewsSnap.documents.mapNotNull { it.getString("productId") }.distinct()
                val count = reviewsSnap.size()

                // 3) Fetch reviewed products (whereIn chunks of 10)
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

                // 4) Follow state
                val isSelf = (me == targetUid)
                val followDoc = db.collection("users").document(me)
                    .collection("following").document(targetUid)
                    .get().awaitk()
                val isFollowing = followDoc.exists()

                _uiState.value = PublicProfileUiState(
                    isLoading = false,
                    profile = profile,
                    reviewCount = count,
                    products = products,
                    isSelf = isSelf,
                    isFollowing = isFollowing
                )
            } catch (e: Exception) {
                _uiState.value = PublicProfileUiState(isLoading = false, error = e.message ?: "Failed to load profile")
            }
        }
    }

    fun follow() {
        val me = auth.currentUser?.uid ?: return
        if (me == targetUid) return
        _uiState.value = _uiState.value.copy(isBusy = true, error = null)

        viewModelScope.launch {
            try {
                db.collection("users").document(me)
                    .collection("following").document(targetUid)
                    .set(
                        mapOf(
                            "targetUid" to targetUid,
                            "followedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .awaitk()
                _uiState.value = _uiState.value.copy(isBusy = false, isFollowing = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBusy = false, error = e.message)
            }
        }
    }

    fun unfollow() {
        val me = auth.currentUser?.uid ?: return
        if (me == targetUid) return
        _uiState.value = _uiState.value.copy(isBusy = true, error = null)

        viewModelScope.launch {
            try {
                db.collection("users").document(me)
                    .collection("following").document(targetUid)
                    .delete()
                    .awaitk()
                _uiState.value = _uiState.value.copy(isBusy = false, isFollowing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBusy = false, error = e.message)
            }
        }
    }
}
