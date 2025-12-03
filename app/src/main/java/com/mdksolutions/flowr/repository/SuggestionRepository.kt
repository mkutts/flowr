package com.mdksolutions.flowr.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.Suggestion
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SuggestionRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col = db.collection("suggestions")

    suspend fun create(s: Suggestion): String {
        val id = s.id.ifBlank { UUID.randomUUID().toString() }

        // 🔧 EXPLICIT MAP: force correct Firestore field names
        val data = hashMapOf(
            "id" to id,
            "userId" to s.userId,
            "type" to s.type,
            "title" to s.title,
            "description" to s.description,
            "severity" to s.severity,
            "stepsToReproduce" to s.stepsToReproduce,
            "contactEmail" to s.contactEmail,
            "screenshotUrl" to s.screenshotUrl,
            "status" to s.status,
            "createdAt" to s.createdAt
        )

        col.document(id).set(data).await()
        return id
    }
}
