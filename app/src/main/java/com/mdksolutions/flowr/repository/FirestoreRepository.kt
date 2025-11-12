package com.mdksolutions.flowr.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject // ✅ KTX mapper
import com.mdksolutions.flowr.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    // ✅ Lazy init so Firestore doesn't start up until first use
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val productsRef by lazy { db.collection("products") }

    // Add a product to Firestore
    suspend fun addProduct(product: Product): Boolean {
        return try {
            val docRef = productsRef.document() // auto-generate ID
            val productWithId = product.copy(id = docRef.id)
            docRef.set(productWithId).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Get all products as a Flow (real-time updates)
    fun getProducts(): Flow<List<Product>> = callbackFlow {
        val registration = productsRef
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    // ✅ KTX generic mapper (no Java no-arg ctor required) + keep Firestore ID
                    doc.toObject<Product>()?.copy(id = doc.id)
                }.orEmpty()

                trySend(products)
            }

        awaitClose { registration.remove() }
    }
}
