package com.mdksolutions.flowr.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.mdksolutions.flowr.ui.screens.profile.PublicProfileScreen
import com.mdksolutions.flowr.ui.screens.profile.FollowingScreen
import com.mdksolutions.flowr.ui.screens.profile.BudtenderWorkEditScreen
import com.mdksolutions.flowr.ui.screens.auth.SignUpScreen
// Suggestion screen
import com.mdksolutions.flowr.ui.screens.support.SuggestionScreen
// Bottom nav component
import com.mdksolutions.flowr.ui.components.FlowrBottomNav

@Composable
fun AppNavGraph(
    navController: NavHostController,
    themeType: FlowrThemeType
) {
    FlowrTheme(themeType = themeType) {

        // Track current route so we can show/hide the bottom bar
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val bottomNavRoutes = setOf("home", "add_product", "profile", "suggest")

        Scaffold(
            bottomBar = {
                if (currentRoute in bottomNavRoutes) {
                    FlowrBottomNav(navController = navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "age_gate",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("age_gate") { AgeGateScreen(navController) }
                composable("auth") { AuthScreen(navController) }
                composable("signup") { SignUpScreen(navController) }
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

                // 🔐 In-app password reset screen
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

                composable(
                    route = "public_profile/{uid}",
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })
                ) {
                    PublicProfileScreen(navController)
                }

                composable(route = "following") {
                    FollowingScreen(navController)
                }

                composable(route = "edit_work") {
                    BudtenderWorkEditScreen(navController)
                }

                // ▼ Feedback/Suggestions
                composable(route = "suggest") {
                    SuggestionScreen(navController)
                }
            }
        }
    }
}
