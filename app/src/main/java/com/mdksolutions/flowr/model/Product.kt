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
    val strainType: String = "",   // ✅ default fixes missing-field crashes
    val states: List<String>? = null,

    // ✅ NEW potency fields (all optional for backward compatibility)
    val potencyUsesMg: Boolean? = null,   // true => show dosage (mg); false => show %; null => fallback to category heuristic
    val thcPercent: Double? = null,       // when potencyUsesMg == false
    val dosageMg: Double? = null,         // when potencyUsesMg == true
    val cbdPercent: Double? = null,
    val cbdMg: Double? = null
)
