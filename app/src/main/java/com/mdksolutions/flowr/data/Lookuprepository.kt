package com.mdksolutions.flowr.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

/**
 * Two supported models:
 *  A) Dedicated lookup collections:
 *     - /lookups/brands/list (subcollection docs with field "name")
 *     - /lookups/strains/list
 *     - /lookups/other/list
 *
 *  B) Derive from products collection (fallback):
 *     - /products (docs have fields "brand", "strain", "other")
 *
 * Prefer A for performance. B will de-dupe client-side if A is empty.
 */
class LookupRepository(
    private val db: FirebaseFirestore
) {
    private val memoryCache = mutableMapOf<String, List<String>>() // key = "brands"| "strains" | "other"

    suspend fun getLookups(kind: String): List<String> {
        memoryCache[kind]?.let { return it }

        // 1) Try dedicated lookup collection first: /lookups/{kind}/list
        val dedicated = readDedicated(kind)
        if (dedicated.isNotEmpty()) {
            memoryCache[kind] = dedicated
            return dedicated
        }

        // 2) Fallback: derive from /products field
        val derived = deriveFromProducts(kindField = kindFieldName(kind))
        memoryCache[kind] = derived
        return derived
    }

    private fun kindFieldName(kind: String): String = when (kind) {
        "brands" -> "brand"
        // ⬇️ If your strain is stored as Product.name, use "name"
        "strains" -> "name"
        else -> "other"
    }

    private suspend fun readDedicated(kind: String): List<String> {
        return try {
            val snap = db.collection("lookups")
                .document(kind)
                .collection("list")
                .get()
                .await()
            snap.toNames()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun deriveFromProducts(kindField: String): List<String> {
        return try {
            // NOTE: Firestore mobile SDK doesn’t support DISTINCT server-side.
            // We select the field client-side and de-dupe.
            val snap = db.collection("products")
                // Consider adding .limit(5000) if you expect very large collections for initial load.
                .get()
                .await()
            snap.documents.mapNotNull { it.getString(kindField)?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun QuerySnapshot.toNames(): List<String> =
        this.documents.mapNotNull { it.getString("name")?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
}
