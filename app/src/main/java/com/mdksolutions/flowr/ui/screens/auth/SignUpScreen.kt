package com.mdksolutions.flowr.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    // --- Form state ---
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // Local validators
    fun emailLooksValid(e: String) = Patterns.EMAIL_ADDRESS.matcher(e).matches()
    fun usernameLooksValid(u: String): Boolean {
        if (u.isBlank()) return false
        if (u.length !in 3..24) return false
        if (u.startsWith(".") || u.endsWith(".")) return false
        if (!u.all { it.isLetterOrDigit() || it == '_' || it == '.' }) return false
        return true
    }
    fun passwordsOk() = password.length >= 6 && password == confirm

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create your account", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' }
                },
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val label = if (passwordVisible) "Hide" else "Show"
                    TextButton(onClick = { passwordVisible = !passwordVisible }) { Text(label) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val label = if (confirmVisible) "Hide" else "Show"
                    TextButton(onClick = { confirmVisible = !confirmVisible }) { Text(label) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val typedEmail = email.trim()
                    val u = username.trim()
                    when {
                        !emailLooksValid(typedEmail) -> {
                            error = "Please enter a valid email address."; return@Button
                        }
                        !usernameLooksValid(u) -> {
                            error = "Username must be 3–24 chars: letters, numbers, '_' or '.', and can’t start/end with a dot."
                            return@Button
                        }
                        !passwordsOk() -> {
                            error = if (password.length < 6)
                                "Password must be at least 6 characters."
                            else
                                "Passwords do not match."
                            return@Button
                        }
                    }

                    isSubmitting = true
                    error = null

                    val storedEmail = typedEmail.lowercase() // canonicalized for Firestore
                    val ul = u.lowercase()

                    // 1) Create Auth user (handles email collision)
                    auth.createUserWithEmailAndPassword(typedEmail, password)
                        .addOnCompleteListener { authTask ->
                            if (!authTask.isSuccessful) {
                                val ex = authTask.exception
                                error = if (ex is FirebaseAuthUserCollisionException) {
                                    "This email is already associated with an account. Try logging in or use a different email."
                                } else {
                                    "Sign up failed: ${ex?.localizedMessage ?: "Unknown error"}"
                                }
                                isSubmitting = false
                                return@addOnCompleteListener
                            }

                            val uid = auth.currentUser?.uid ?: run {
                                error = "No authenticated user found after sign up."
                                isSubmitting = false
                                return@addOnCompleteListener
                            }

                            // 2) Atomically claim the username by creating /usernames/{usernameLower}
                            // If it already exists, this write is denied by security rules.
                            val usernameDoc = db.collection("usernames").document(ul)
                            val claim = mapOf(
                                "uid" to uid,
                                "username" to u,
                                "createdAt" to System.currentTimeMillis()
                            )
                            usernameDoc.set(claim)
                                .addOnSuccessListener {
                                    // 3) Now create the user's profile doc
                                    val profile = hashMapOf(
                                        "uid" to uid,
                                        "email" to storedEmail,
                                        "displayName" to "",
                                        "username" to u,
                                        "usernameLower" to ul,
                                        "role" to "user",
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    db.collection("users").document(uid).set(profile)
                                        .addOnSuccessListener {
                                            isSubmitting = false
                                            navController.navigate("role_selection") {
                                                popUpTo("auth") { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener { ex ->
                                            error = "Failed to save profile: ${ex.localizedMessage}"
                                            isSubmitting = false
                                        }
                                }
                                .addOnFailureListener { ex ->
                                    // Likely taken -> clean up the just-created auth user to avoid orphan.
                                    val msg = ex.localizedMessage ?: "Unknown"
                                    error = if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                                        "That username is taken."
                                    } else {
                                        "Could not reserve username: $msg"
                                    }
                                    // Try to delete the newly created auth user (best effort)
                                    auth.currentUser?.delete()
                                    isSubmitting = false
                                }
                        }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) "Creating..." else "Create Account")
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Back to Login") }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
