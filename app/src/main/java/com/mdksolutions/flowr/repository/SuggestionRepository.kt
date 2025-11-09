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
        val id = if (s.id.isBlank()) UUID.randomUUID().toString() else s.id
        val toSave = s.copy(id = id)
        col.document(id).set(toSave).await()
        return id
    }
}
