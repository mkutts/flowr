package com.mdksolutions.flowr.ui.screens.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Splash Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("age_gate") }) {
                Text("Continue")
            }
        }
    }
}
