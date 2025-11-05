package com.mdksolutions.flowr.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
                    val data = doc.data ?: return@mapNotNull null

                    // Safely coerce rating to Double regardless of how it was stored
                    val ratingAny = data["rating"]
                    val rating = when (ratingAny) {
                        is Number -> ratingAny.toDouble()
                        else -> 0.0
                    }

                    Review(
                        id = doc.id,
                        productId = data["productId"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        userName = data["userName"] as? String ?: "",
                        rating = rating,
                        feels = (data["feels"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        activity = data["activity"] as? String ?: "",
                        reportedTHC = (data["reportedTHC"] as? Number)?.toDouble(),
                        dosageMg = (data["dosageMg"] as? Number)?.toDouble(),
                        reviewText = data["reviewText"] as? String,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } ?: emptyList()


                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }
}
