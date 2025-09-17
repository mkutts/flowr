package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FollowingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val users: List<UserProfile> = emptyList()
)

class FollowingViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState

    init { refresh() }

    fun refresh() {
        val me = auth.currentUser
        if (me == null) {
            _uiState.value = FollowingUiState(isLoading = false, error = "Not signed in")
            return
        }

        _uiState.value = FollowingUiState(isLoading = true)

        viewModelScope.launch {
            try {
                // 1) Read my following docs: /users/{me}/following/{targetUid}
                val followSnap = db.collection("users").document(me.uid)
                    .collection("following")
                    .get()
                    .awaitk()

                val targetUids = followSnap.documents.map { it.id }.distinct()
                if (targetUids.isEmpty()) {
                    _uiState.value = FollowingUiState(isLoading = false, users = emptyList())
                    return@launch
                }

                // 2) Fetch target profiles in chunks of 10 (whereIn limit)
                val results = mutableListOf<UserProfile>()
                for (chunk in targetUids.chunked(10)) {
                    val qs = db.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .awaitk()
                    results += qs.documents.mapNotNull { d ->
                        d.toObject(UserProfile::class.java)?.copy(uid = d.id)
                    }
                }

                _uiState.value = FollowingUiState(isLoading = false, users = results)
            } catch (e: Exception) {
                _uiState.value = FollowingUiState(isLoading = false, error = e.message ?: "Failed to load following")
            }
        }
    }
}
