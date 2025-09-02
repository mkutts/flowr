package com.mdksolutions.flowr.ui.screens.roleselection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions   // ✅ correct
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Date

@Composable
fun RoleSelectionScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCodeDialog by remember { mutableStateOf(false) }

    // Solid theme background so the window logo never bleeds through
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select Your Role",
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        saveRoleAndNavigate(
                            "enthusiast", auth, db, navController,
                            { isLoading = it }, { errorMessage = it }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("I'm an Enthusiast") }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { showCodeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("I'm a Budtender") }

                if (isLoading) {
                    Spacer(Modifier.height(16.dp))
                    Text("Saving…", color = MaterialTheme.colorScheme.onBackground)
                }
                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showCodeDialog) {
        BudtenderCodeDialog(
            onConfirm = { code ->
                val user = auth.currentUser
                if (user == null) {
                    errorMessage = "User not logged in"
                    return@BudtenderCodeDialog
                }
                isLoading = true
                verifyAndRedeemPromoCode(
                    db = db,
                    codeRaw = code,
                    userId = user.uid
                ) { ok, msg, payload ->
                    isLoading = false
                    if (!ok) {
                        errorMessage = msg ?: "Invalid code"
                        return@verifyAndRedeemPromoCode
                    }
                    saveRoleAndNavigateWithPromo(
                        role = "budtender",
                        promo = payload!!,
                        auth = auth,
                        db = db,
                        navController = navController,
                        setLoading = { isLoading = it },
                        setError = { errorMessage = it }
                    )
                    showCodeDialog = false
                }
            },
            onDismiss = { showCodeDialog = false }
        )
    }
}

@Composable
private fun BudtenderCodeDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Budtender Promo Code") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Promo code") },
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(code.trim()) }) { Text("Verify") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Option 1: Do NOT update promo_codes from the client.
 * - Validates the code (exists, active, not expired)
 * - Prevents duplicate redemption per user
 * - Records redemption in promo_code_uses
 * Usage counts can be aggregated later from promo_code_uses or via a Cloud Function.
 */
private fun verifyAndRedeemPromoCode(
    db: FirebaseFirestore,
    codeRaw: String,
    userId: String,
    onResult: (ok: Boolean, message: String?, payload: Map<String, Any?>?) -> Unit
) {
    val codeId = codeRaw.trim().lowercase()
    if (codeId.isEmpty()) {
        onResult(false, "Enter a code", null); return
    }

    val codeRef = db.collection("promo_codes").document(codeId)
    val useRef = db.collection("promo_code_uses").document("$userId-$codeId")

    db.runTransaction { txn ->
        val codeSnap = txn.get(codeRef)
        if (!codeSnap.exists()) throw IllegalArgumentException("Code not found")

        val active = codeSnap.getBoolean("active") ?: false
        if (!active) throw IllegalStateException("Code is inactive")

        val expiresAt = codeSnap.getTimestamp("expiresAt")
        if (expiresAt != null && expiresAt.toDate().time < System.currentTimeMillis()) {
            throw IllegalStateException("Code expired")
        }

        // NOTE: We are NOT enforcing maxUses here (no client writes to promo_codes).
        // To enforce limits, add a Cloud Function that increments a counter on new uses.

        // prevent a user from redeeming the same code twice
        if (txn.get(useRef).exists()) {
            throw IllegalStateException("You already used this code")
        }

        // record redemption only
        txn.set(useRef, mapOf(
            "codeId" to codeId,
            "userId" to userId,
            "usedAt" to Timestamp.now()
        ))

        // Return metadata to store on the user record
        mapOf(
            "codeId" to codeId,
            "label" to codeSnap.getString("label"),
            "dispensaryId" to codeSnap.getString("dispensaryId")
        )
    }.addOnSuccessListener { payload ->
        @Suppress("UNCHECKED_CAST")
        onResult(true, null, payload as Map<String, Any?>)
    }.addOnFailureListener { e ->
        onResult(false, e.message, null)
    }
}

fun saveRoleAndNavigate(
    role: String,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navController: NavController,
    setLoading: (Boolean) -> Unit,
    setError: (String) -> Unit
) {
    val user = auth.currentUser
    if (user != null) {
        setLoading(true)
        val userData = hashMapOf(
            "email" to user.email,
            "role" to role,
            "created_at" to Date()
        )
        db.collection("users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                setLoading(false)
                navController.navigate("home") {
                    popUpTo("role_selection") { inclusive = true }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                setError("Error saving role: ${e.message}")
            }
    } else {
        setError("User not logged in")
    }
}

fun saveRoleAndNavigateWithPromo(
    role: String,
    promo: Map<String, Any?>,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    navController: NavController,
    setLoading: (Boolean) -> Unit,
    setError: (String) -> Unit
) {
    val user = auth.currentUser ?: return setError("User not logged in")
    setLoading(true)
    val userData = mapOf(
        "email" to user.email,
        "role" to role,
        "promoCode" to mapOf(
            "codeId" to promo["codeId"],
            "label" to promo["label"],
            "dispensaryId" to promo["dispensaryId"],
            "usedAt" to Timestamp.now()
        ),
        "updated_at" to Date()
    )
    db.collection("users").document(user.uid)
        .set(userData, SetOptions.merge())
        .addOnSuccessListener {
            setLoading(false)
            navController.navigate("home") {
                popUpTo("role_selection") { inclusive = true }
            }
        }
        .addOnFailureListener { e ->
            setLoading(false); setError("Error saving role: ${e.message}")
        }
}
