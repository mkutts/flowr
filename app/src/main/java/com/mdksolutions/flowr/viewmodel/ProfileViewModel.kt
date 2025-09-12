package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: UserProfile? = null,
    val reviewCount: Int = 0
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { refresh() }

    fun refresh() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = ProfileUiState(isLoading = false, error = "Not signed in")
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val uid = user.uid
                val docRef = db.collection("users").document(uid)
                val snap = docRef.get().awaitk()

                val profile = if (snap.exists()) {
                    snap.toObject(UserProfile::class.java)?.copy(uid = uid)
                } else {
                    val seeded = UserProfile(
                        uid = uid,
                        displayName = user.displayName ?: user.email ?: "",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString()
                    )
                    docRef.set(seeded).awaitk()
                    seeded
                }

                // Count this user's reviews
                val qs = db.collection("reviews")
                    .whereEqualTo("userId", uid)
                    .get()
                    .awaitk()
                val count = qs.size()

                _uiState.value = ProfileUiState(
                    isLoading = false,
                    profile = profile,
                    reviewCount = count
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val p = _uiState.value.profile ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(p.uid)
                    .update(mapOf("displayName" to newName))
                    .awaitk()
                _uiState.update { it.copy(profile = it.profile?.copy(displayName = newName)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

suspend fun <T> Task<T>.awaitk(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result)
        else cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
    }
}
