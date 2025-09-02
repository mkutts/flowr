package com.mdksolutions.flowr.ui.screens.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// 🆕 imports
import android.util.Patterns
import com.google.firebase.auth.EmailAuthProvider

@Composable
fun AuthScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) } // 👈 toggle state

    // 🔁 Forgot password with enumeration-safe UX
    fun sendPasswordReset() {
        val trimmed = email.trim()

        if (trimmed.isEmpty()) {
            errorMessage = "Please enter your email first."
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            errorMessage = "Please enter a valid email address."
            return
        }

        isLoading = true
        errorMessage = ""
        infoMessage = ""

        auth.fetchSignInMethodsForEmail(trimmed)
            .addOnCompleteListener { methodsTask ->
                if (!methodsTask.isSuccessful) {
                    isLoading = false
                    errorMessage = "Couldn't verify email right now. Please try again."
                    return@addOnCompleteListener
                }

                val methods = methodsTask.result?.signInMethods.orEmpty()
                val supportsPassword = methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)

                if (supportsPassword) {
                    // Only send reset if account exists AND supports password sign-in
                    auth.sendPasswordResetEmail(trimmed)
                        .addOnCompleteListener {
                            isLoading = false
                            // Generic, non-enumerating success message
                            infoMessage = "If an account exists for $trimmed, you'll receive a password reset link shortly."
                            showResetDialog = true
                        }
                } else {
                    // Do not send; still show generic message to avoid revealing account existence
                    isLoading = false
                    infoMessage = "If an account exists for $trimmed, you'll receive a password reset link shortly. If you originally used Google/Apple, try that provider."
                    showResetDialog = true
                }
            }
    }

    // ✅ Solid background like AgeGateScreen
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Login / Sign Up",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val desc = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc)
                        }
                    }
                )

                // Forgot password row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    TextButton(
                        onClick = { sendPasswordReset() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Forgot password?")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = {
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        db.collection("users").document(user.uid).get()
                                            .addOnSuccessListener { document ->
                                                isLoading = false
                                                if (document.exists()) {
                                                    navController.navigate("home") {
                                                        popUpTo("auth") { inclusive = true }  // clear login from back stack
                                                        launchSingleTop = true
                                                    }
                                                } else {
                                                    navController.navigate("role_selection") {
                                                        popUpTo("auth") { inclusive = true }
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "User check failed: ${e.message}"
                                            }
                                    } else {
                                        isLoading = false
                                        errorMessage = "No authenticated user found after sign up."
                                    }
                                } else {
                                    errorMessage = "Sign Up Failed: ${task.exception?.message}"
                                    isLoading = false
                                }
                            }
                    }) {
                        Text("Sign Up")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        db.collection("users").document(user.uid).get()
                                            .addOnSuccessListener { document ->
                                                isLoading = false
                                                if (document.exists()) {
                                                    navController.navigate("home")
                                                } else {
                                                    navController.navigate("role_selection")
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "User check failed: ${e.message}"
                                            }
                                    } else {
                                        isLoading = false
                                        errorMessage = "No authenticated user found after login."
                                    }
                                } else {
                                    errorMessage = "Login Failed: ${task.exception?.message}"
                                    isLoading = false
                                }
                            }
                    }) {
                        Text("Login")
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Success dialog (shows generic message)
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Check your email") },
                    text = { Text(infoMessage) },
                    confirmButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
