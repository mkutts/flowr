package com.mdksolutions.flowr.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.mdksolutions.flowr.ui.screens.agegate.AgeGateScreen
import com.mdksolutions.flowr.ui.screens.auth.AuthScreen
import com.mdksolutions.flowr.ui.screens.auth.ResetPasswordScreen
import com.mdksolutions.flowr.ui.screens.home.AddProductScreen
import com.mdksolutions.flowr.ui.screens.roleselection.RoleSelectionScreen
import com.mdksolutions.flowr.ui.screens.home.HomeScreen
import com.mdksolutions.flowr.ui.screens.productdetail.AddReviewScreen
import com.mdksolutions.flowr.ui.screens.productdetail.ProductDetailScreen
import com.mdksolutions.flowr.ui.screens.profile.ProfileScreen
import com.mdksolutions.flowr.ui.theme.FlowrTheme
import com.mdksolutions.flowr.ui.theme.FlowrThemeType
import com.mdksolutions.flowr.ui.screens.reviews.MyReviewsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    themeType: FlowrThemeType
) {
    FlowrTheme(themeType = themeType) {
        NavHost(
            navController = navController,
            startDestination = "age_gate"
        ) {
            composable("age_gate") { AgeGateScreen(navController) }
            composable("auth") { AuthScreen(navController) }
            composable("role_selection") { RoleSelectionScreen(navController) }

            // ✅ Scope HomeScreen's ViewModel to this back stack entry
            composable("home") { backStackEntry ->
                HomeScreen(
                    navController = navController,
                    backStackEntry = backStackEntry
                )
            }

            composable("add_product") { AddProductScreen(navController) }

            composable("product_detail/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                ProductDetailScreen(navController, productId)
            }

            composable("profile") { ProfileScreen(navController) }

            composable("add_review/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                AddReviewScreen(navController, productId)
            }

            // 🔐 New route: in-app password reset screen
            composable(
                route = "reset_password?oob={oob}",
                arguments = listOf(navArgument("oob") { type = NavType.StringType; nullable = false })
            ) { backStackEntry ->
                val oobCode = backStackEntry.arguments?.getString("oob")!!
                ResetPasswordScreen(navController, oobCode)
            }

            composable(route = "my_reviews") {
                MyReviewsScreen(navController)
            }

        }
    }
}
