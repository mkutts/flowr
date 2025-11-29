package com.mdksolutions.flowr.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "ReviewRepository"

class ReviewRepository {

    private val db = FirebaseFirestore.getInstance()
    private val reviewsRef = db.collection("reviews")

    // Add a review to Firestore
    suspend fun addReview(review: Review): Boolean {
        return try {
            val docRef = reviewsRef.document() // Auto-generate ID

            Log.d(
                TAG,
                "addReview(): writing doc at path=${docRef.path} " +
                        "projectId=${db.app.options.projectId} " +
                        "productId=${review.productId}, userId=${review.userId}"
            )

            // 🔧 EXPLICIT MAP so Firestore field names are correct
            val data = hashMapOf(
                "id" to docRef.id,
                "productId" to review.productId,
                "userId" to review.userId,
                "userName" to review.userName,
                "rating" to review.rating,
                "feels" to review.feels,
                "activity" to review.activity,
                "reportedTHC" to review.reportedTHC,
                "dosageMg" to review.dosageMg,
                "reviewText" to review.reviewText,
                "createdAt" to review.createdAt
            )

            docRef.set(data).await()

            Log.d(TAG, "addReview(): SUCCESS docId=${docRef.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "addReview(): ERROR while writing review", e)
            false
        }
    }

    // Get reviews for a specific product (real-time)
    fun getReviews(productId: String): Flow<List<Review>> = callbackFlow {
        Log.d(TAG, "getReviews(): listening for productId=$productId")

        val listener = reviewsRef
            .whereEqualTo("productId", productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getReviews(): snapshot error", error)
                    close(error)
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                Log.d(TAG, "getReviews(): snapshot has ${docs.size} docs for productId=$productId")

                docs.forEach { doc ->
                    Log.d(
                        TAG,
                        "getReviews(): docId=${doc.id}, productIdField=${doc.getString("productId")}"
                    )
                }

                val reviews = docs
                    .mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null

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
                            createdAt = (data["createdAt"] as? Number)?.toLong()
                                ?: System.currentTimeMillis()
                        )
                    }
                    .sortedByDescending { it.createdAt }

                Log.d(TAG, "getReviews(): mapped ${reviews.size} reviews for productId=$productId")
                trySend(reviews)
            }

        awaitClose { listener.remove() }
    }
}
