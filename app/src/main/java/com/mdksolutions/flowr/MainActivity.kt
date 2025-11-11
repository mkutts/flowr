package com.mdksolutions.flowr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mdksolutions.flowr.navigation.AppNavGraph
import com.mdksolutions.flowr.ui.theme.FlowrThemeType
import com.mdksolutions.flowr.util.ensurePlayServices
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Ads
import com.mdksolutions.flowr.ui.components.BannerAd
import com.mdksolutions.flowr.ads.RewardedAds

class MainActivity : ComponentActivity() {

    private lateinit var navControllerRef: NavHostController

    private val resetHosts = setOf(
        "flowr-f5248.web.app",
        "flowr-f5248.firebaseapp.com"
    )

    private val acceptedPathPrefixes = listOf(
        "/auth/reset",
        "/__/auth/action"
    )

    private var lastHandledDeepLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)

        setContent {
            val activity = this@MainActivity

            // Preload a rewarded ad once UI starts (Home FAB will use it)
            LaunchedEffect(Unit) { RewardedAds.load(activity) }

            var playServicesOk by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                playServicesOk = ensurePlayServices(activity)
            }
            LaunchedEffect(playServicesOk) {
                if (playServicesOk != null) keepSplash = false
            }

            when (playServicesOk) {
                null -> { /* splash showing */ }
                true -> {
                    navControllerRef = rememberNavController()
                    val currentTheme = remember { mutableStateOf(FlowrThemeType.DARK_LUXURY) }

                    Scaffold(
                        // ✅ Banner ad always at the top
                        topBar = {
                            Surface(tonalElevation = 1.dp) {
                                BannerAd(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AppNavGraph(
                                navController = navControllerRef,
                                themeType = currentTheme.value
                            )
                        }

                        // Handle deep link on cold start
                        LaunchedEffect(Unit) {
                            intent?.let { handleResetLink(navControllerRef, it) }
                        }
                    }
                }
                false -> {
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
                            }) { Text("Retry") }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Default) {
            // post-first-frame warmups
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navControllerRef.isInitialized) {
            handleResetLink(navControllerRef, intent)
        }
    }

    private fun handleResetLink(navController: NavController, intent: Intent) {
        val uri: Uri = intent.data ?: return

        val linkKey = uri.toString()
        if (lastHandledDeepLink == linkKey) {
            Log.d("FPW", "Deep link already handled, skipping: $linkKey")
            return
        }

        if (uri.scheme != "https") return
        val host = uri.host ?: return
        if (host !in resetHosts) return

        val path = uri.path.orEmpty()
        if (acceptedPathPrefixes.none { prefix -> path.startsWith(prefix) }) return

        val mode = uri.getQueryParameter("mode")
        val oob  = uri.getQueryParameter("oobCode")

        Log.d("FPW", "Deep link matched host=$host path=$path mode=$mode oob=${oob?.take(6)}…")

        if (mode == "resetPassword" && !oob.isNullOrBlank()) {
            lastHandledDeepLink = linkKey
            navController.navigate("reset_password?oob=${Uri.encode(oob)}") {
                launchSingleTop = true
            }
        }
    }
}
