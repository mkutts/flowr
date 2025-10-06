package com.mdksolutions.flowr.model

data class Review(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val feels: List<String> = emptyList(),  // ["Happy", "Relaxed"]
    val activity: String = "",             // "Makes me want to..."
    val reportedTHC: Double? = null,       // Optional
    // NEW: optional free-text body
    val reviewText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
