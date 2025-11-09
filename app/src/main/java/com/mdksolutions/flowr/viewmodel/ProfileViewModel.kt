package com.mdksolutions.flowr.viewmodel

import android.net.Uri
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.firestore.FirebaseFirestoreException

import com.google.firebase.firestore.SetOptions
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: UserProfile? = null,
    val reviewCount: Int = 0,
    val isUploading: Boolean = false           // ⬅️ show upload progress in UI
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

                // Helper: look up the claimed username (if any) from /usernames
                suspend fun fetchClaimedUsername(): Pair<String, String> {
                    val qs = db.collection("usernames")
                        .whereEqualTo("uid", uid)
                        .limit(1)
                        .get()
                        .awaitk()
                    val claim = qs.documents.firstOrNull()
                    val uname = claim?.getString("username").orEmpty()
                    val lower = claim?.id.orEmpty() // document id is the lowercase username
                    return uname to lower
                }

                val profile: UserProfile = if (snap.exists()) {
                    // Existing profile
                    val loaded = snap.toObject(UserProfile::class.java)?.copy(uid = uid)
                        ?: UserProfile(uid = uid)

                    // Backfill username fields if missing/blank
                    val ensuredUsernames = if (loaded.username.isBlank() || loaded.usernameLower.isBlank()) {
                        val (uname, lower) = fetchClaimedUsername()
                        if (uname.isNotBlank()) {
                            docRef.set(
                                mapOf("username" to uname, "usernameLower" to lower),
                                SetOptions.merge()
                            ).awaitk()
                            loaded.copy(username = uname, usernameLower = lower)
                        } else {
                            loaded
                        }
                    } else {
                        loaded
                    }

                    // ✅ Backfill displayName_lc if missing
                    val existingLc = snap.getString("displayName_lc").orEmpty()
                    val displayNameClean = ensuredUsernames.displayName.trim()
                    if (displayNameClean.isNotEmpty() && existingLc.isBlank()) {
                        docRef.set(
                            mapOf("displayName_lc" to displayNameClean.lowercase()),
                            SetOptions.merge()
                        ).awaitk()
                    }

                    ensuredUsernames
                } else {
                    // Seed brand-new profile, including claimed username if present
                    val (uname, lower) = fetchClaimedUsername()
                    val cleanName = (user.displayName ?: user.email ?: "").trim()

                    val seeded = UserProfile(
                        uid = uid,
                        displayName = cleanName,
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString(),
                        username = uname,
                        usernameLower = lower
                    )
                    // Write the main doc
                    docRef.set(seeded).awaitk()
                    // ✅ Also persist the lowercase display name for search
                    if (cleanName.isNotEmpty()) {
                        docRef.set(
                            mapOf("displayName_lc" to cleanName.lowercase()),
                            SetOptions.merge()
                        ).awaitk()
                    }
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

    // ✅ Update both displayName and displayName_lc
    fun updateDisplayName(newName: String) {
        val p = _uiState.value.profile ?: return
        val clean = newName.trim()
        viewModelScope.launch {
            try {
                db.collection("users").document(p.uid)
                    .update(
                        mapOf(
                            "displayName" to clean,
                            "displayName_lc" to clean.lowercase()
                        )
                    )
                    .awaitk()
                _uiState.update {
                    it.copy(profile = it.profile?.copy(displayName = clean))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateUsername(newName: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = auth.currentUser?.uid
        val current = _uiState.value.profile
        if (uid == null || current == null) {
            onResult(false, "Not signed in")
            return
        }

        val trimmed = newName.trim()
        val newLower = trimmed.lowercase()

        // quick client-side guardrails
        val valid = trimmed.length in 3..24 &&
                !trimmed.startsWith(".") &&
                !trimmed.endsWith(".") &&
                trimmed.all { it.isLetterOrDigit() || it == '_' || it == '.' }
        if (!valid) {
            onResult(false, "Invalid username format")
            return
        }

        val oldLower = current.usernameLower

        viewModelScope.launch {
            try {
                // 1) Atomically claim the new username
                val claimRef = db.collection("usernames").document(newLower)
                val claimData = mapOf(
                    "uid" to uid,
                    "username" to trimmed,
                    "createdAt" to System.currentTimeMillis()
                )
                claimRef.set(claimData).awaitk()

                // 2) Update the user's profile with the new username
                db.collection("users").document(uid)
                    .update(mapOf("username" to trimmed, "usernameLower" to newLower))
                    .awaitk()

                // 3) Best-effort: delete old username claim if it belonged to this user
                if (oldLower.isNotBlank() && oldLower != newLower) {
                    val oldRef = db.collection("usernames").document(oldLower)
                    val snap = oldRef.get().awaitk()
                    if (snap.exists() && snap.getString("uid") == uid) {
                        oldRef.delete().awaitk()
                    }
                }

                // 4) Update local UI state
                _uiState.update {
                    it.copy(profile = it.profile?.copy(username = trimmed, usernameLower = newLower))
                }
                onResult(true, null)
            } catch (e: FirebaseFirestoreException) {
                val msg =
                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS
                    ) "That username is taken."
                    else e.localizedMessage ?: "Unknown error"
                onResult(false, msg)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    // Upload avatar to Storage and save download URL in /users/{uid}.photoUrl
    fun uploadProfilePhoto(uri: Uri) {
        val user = auth.currentUser ?: run {
            _uiState.update { it.copy(error = "Not signed in") }
            return
        }
        val uid = user.uid
        val ref = storage.reference.child("user_photos/$uid/avatar.jpg")

        _uiState.update { it.copy(isUploading = true, error = null) }

        viewModelScope.launch {
            try {
                Log.d("ProfileVM", "Uploading to: ${ref.path}")
                ref.putFile(uri).awaitk()

                val url = ref.downloadUrl.awaitk().toString()

                db.collection("users").document(uid)
                    .set(mapOf("photoUrl" to url), SetOptions.merge())
                    .awaitk()

                _uiState.update { s ->
                    s.copy(isUploading = false, profile = s.profile?.copy(photoUrl = url))
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Upload flow failed", e)
                _uiState.update {
                    it.copy(isUploading = false, error = e.message ?: "Upload failed")
                }
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

// --- Await helpers ---
suspend fun <T> Task<T>.awaitk(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result)
        else cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
    }
}

// Await for UploadTask (Firebase Storage)
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun UploadTask.awaitk(): UploadTask.TaskSnapshot =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { snap -> cont.resume(snap) {} }
        addOnFailureListener { ex -> cont.resumeWithException(ex) }
    }
