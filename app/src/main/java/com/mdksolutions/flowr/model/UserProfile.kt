package com.mdksolutions.flowr.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
/**
 * Firestore document: users/{uid}
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val role: String = "user",         // "user" | "budtender" | "influencer" (future)
    val createdAt: Long = System.currentTimeMillis()
)
