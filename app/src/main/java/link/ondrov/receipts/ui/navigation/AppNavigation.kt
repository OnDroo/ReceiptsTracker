package link.ondrov.receipts.ui.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import link.ondrov.receipts.ui.screens.DashboardScreen
import link.ondrov.receipts.ui.screens.EditReceiptScreen
import link.ondrov.receipts.ui.screens.ReceiptDetailScreen
import link.ondrov.receipts.ui.viewmodels.DashboardViewModel
import link.ondrov.receipts.ui.viewmodels.ReceiptDetailViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val animDuration = 400

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,

        // 1. OPENING A SCREEN
        // New screen comes in from the Right
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }, // Starts completely off-screen right
                animationSpec = tween(animDuration)
            )
        },
        // Old screen shifts slightly to the Left (Parallax)
        // It moves only 25% of the width, creating the "depth" effect
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(animDuration)
            ) + fadeOut(animationSpec = tween(animDuration)) // Fade helps it recede
        },

        // 2. GOING BACK (The "Peek" Effect)
        // The screen underneath slides back in from the Left.
        // KEY FIX: We only start it at -25%. It doesn't travel "far."
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(animDuration)
            ) + fadeIn(animationSpec = tween(animDuration))
        },
        // The current screen slides all the way out to the Right
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(animDuration)
            )
        }
    ) {
        // --- DASHBOARD ---
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onOpenReceipts = { /* TODO: Open List */ },
                onReceiptClick = { id ->
                    // Standard navigation (viewing existing receipt)
                    navController.navigate(Screen.ReceiptDetails.createRoute(id))
                },
                onNewScan = { id, encodedUri ->
                    // Scan navigation (passing the new image URI)
                    navController.navigate(Screen.ReceiptDetails.createRoute(id, encodedUri))
                }
            )
        }

        // --- DETAILS ---
        composable(
            route = Screen.ReceiptDetails.route,
            arguments = listOf(
                navArgument("receiptId") { type = NavType.StringType },
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getString("receiptId") ?: return@composable
            val imageUri = backStackEntry.arguments?.getString("imageUri")

            ReceiptDetailScreen(
                receiptId = receiptId,
                imageUriArg = imageUri,
                onBack = { navController.popBackStack() }
            )
        }

        // --- EDIT ---
        composable(
            route = Screen.ReceiptEdit.route,
            arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
        ) {
            // Using the Factory to get the ViewModel
            val viewModel: ReceiptDetailViewModel = viewModel(factory = ReceiptDetailViewModel.Factory)
            val uiState by viewModel.uiState.collectAsState()

            uiState?.let { receipt ->
                EditReceiptScreen(
                    originalReceipt = receipt,
                    onDismiss = { navController.popBackStack() },
                    onSave = { updated ->
                        viewModel.updateReceipt(updated) { navController.popBackStack() }
                    }
                )
            }
        }
    }
}