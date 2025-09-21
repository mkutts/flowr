package com.mdksolutions.flowr.model

// A single shift window using 24h "HH:mm" strings (e.g., "09:00"–"17:30")
data class WorkShift(
    val start: String = "",
    val end: String = ""
)

// Budtender's work info stored at: /users/{uid}/work
data class BudtenderWork(
    val dispensaryName: String = "",
    val address: String = "",        // Freeform address (used for Maps if no placeId)
    val placeId: String? = null,     // Optional Google Places ID (future step)
    // Map of day -> list of shifts. Keys should be lowercase: monday..sunday
    val schedule: Map<String, List<WorkShift>> = emptyMap()
)

// Helpers for UI
val WEEK_DAYS = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
)
