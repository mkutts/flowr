package com.mdksolutions.flowr.model

data class Suggestion(
    val id: String = "",
    val userId: String = "anonymous",
    val type: String = "Feature", // "Feature" | "Bug"
    val title: String = "",
    val description: String = "",
    val severity: String? = null,           // Bug-only: "Low" | "Med" | "High" | "Critical"
    val stepsToReproduce: String? = null,   // Bug-only
    val contactEmail: String? = null,
    val screenshotUrl: String? = null,
    val status: String = "open",
    val createdAt: Long = System.currentTimeMillis()
)
