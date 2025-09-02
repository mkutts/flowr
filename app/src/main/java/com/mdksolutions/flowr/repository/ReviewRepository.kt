package com.mdksolutions.flowr.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject   // ✅ KTX mapper
import com.mdksolutions.flowr.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReviewRepository {

    private val db = FirebaseFirestore.getInstance()
    private val reviewsRef = db.collection("reviews")

    // Add a review to Firestore
    suspend fun addReview(review: Review): Boolean {
        return try {
            val docRef = reviewsRef.document() // Auto-generate ID
            val reviewWithId = review.copy(id = docRef.id)
            docRef.set(reviewWithId).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Get reviews for a specific product (real-time)
    fun getReviews(productId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviewsRef
            .whereEqualTo("productId", productId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    // ✅ KTX generic mapper + keep Firestore ID
                    doc.toObject<Review>()?.copy(id = doc.id)
                } ?: emptyList()

                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }
}
