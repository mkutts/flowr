// Strain.kt
package com.example.flowr.models

data class Strain(
    val name: String = "",
    val type: String = "",       // e.g., indica, sativa, or hybrid
    val effects: List<String> = listOf(),  // e.g., ["relaxed", "happy"]
    val flavors: List<String> = listOf(),  // e.g., ["berry", "sweet"]
    val avgRating: Double = 0.0
)
