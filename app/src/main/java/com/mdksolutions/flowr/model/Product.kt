package com.mdksolutions.flowr.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val state: String = "",
    val avgTHC: Double = 0.0,
    val avgRating: Double = 0.0,
    val topFeels: List<String> = emptyList(),
    val topActivities: List<String> = emptyList(),
    val strainType: String = ""   // ✅ default fixes missing-field crashes
)
