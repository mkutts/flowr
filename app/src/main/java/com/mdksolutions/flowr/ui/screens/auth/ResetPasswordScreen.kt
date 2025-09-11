package com.mdksolutions.flowr.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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

@Composable
fun ResetPasswordScreen(navController: NavController, oobCode: String) {
    val auth = FirebaseAuth.getInstance()

    var verifying by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf<String?>(null) }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var showNewPass by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }

    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    // Verify the code and get the target email
    LaunchedEffect(oobCode) {
        auth.verifyPasswordResetCode(oobCode)
            .addOnSuccessListener { emailAddr ->
                email = emailAddr
                verifying = false
            }
            .addOnFailureListener {
                error = "This password reset link is invalid or has expired."
                verifying = false
            }
    }

    fun submit() {
        error = null
        info = null

        if (newPass.length < 6) {
            error = "Password must be at least 6 characters."
            return
        }
        if (newPass != confirmPass) {
            error = "Passwords do not match."
            return
        }

        submitting = true
        auth.confirmPasswordReset(oobCode, newPass)
            .addOnCompleteListener { task ->
                submitting = false
                if (task.isSuccessful) {
                    info = "Password updated. You can now sign in."
                } else {
                    error = "Could not update password: ${task.exception?.localizedMessage ?: "Unknown error"}"
                }
            }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                verifying -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Set a new password",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(6.dp))
                        email?.let {
                            Text(text = "for $it", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New password") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val icon = if (showNewPass) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val desc = if (showNewPass) "Hide password" else "Show password"
                                IconButton(onClick = { showNewPass = !showNewPass }) {
                                    Icon(icon, contentDescription = desc)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = { Text("Confirm password") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            visualTransformation = if (showConfirmPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val icon = if (showConfirmPass) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val desc = if (showConfirmPass) "Hide password" else "Show password"
                                IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
                                    Icon(icon, contentDescription = desc)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { submit() },
                            enabled = !submitting && error == null
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Update password")
                        }

                        if (error != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                        }
                        if (info != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(info!!, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                navController.navigate("auth") {
                                    launchSingleTop = true
                                }
                            }) {
                                Text("Back to Sign In")
                            }
                        }
                    }
                }
            }
        }
    }
}
