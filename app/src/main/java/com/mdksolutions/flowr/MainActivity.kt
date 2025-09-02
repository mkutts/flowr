package com.mdksolutions.flowr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.mdksolutions.flowr.navigation.AppNavGraph
import com.mdksolutions.flowr.ui.theme.FlowrThemeType
import com.mdksolutions.flowr.util.ensurePlayServices   // ⬅️ from Step 1
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep native splash until we decide Play Services status
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)

        setContent {
            // null = unknown (checking), true = OK, false = not available
            var playServicesOk by remember { mutableStateOf<Boolean?>(null) }

            // Run the check once after first composition
            LaunchedEffect(Unit) {
                playServicesOk = ensurePlayServices(this@MainActivity)
            }

            // Once we know the result, release splash
            LaunchedEffect(playServicesOk) {
                if (playServicesOk != null) keepSplash = false
            }

            when (playServicesOk) {
                null -> {
                    // Still checking — native splash is showing, render nothing.
                }
                true -> {
                    // ✅ Normal app content
                    val navController = rememberNavController()
                    val currentTheme = remember { mutableStateOf(FlowrThemeType.DARK_LUXURY) }
                    AppNavGraph(navController = navController, themeType = currentTheme.value)
                }
                false -> {
                    // ❌ Block with a friendly UI and let the user retry
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Google Play services is required to sign in and use Flowr.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                playServicesOk = ensurePlayServices(this@MainActivity)
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // 🔁 Non‑critical warmups AFTER the first frame
        lifecycleScope.launch(Dispatchers.Default) {
            // e.g., Remote Config fetch, analytics warmup, lightweight prefetches
        }
    }
}
