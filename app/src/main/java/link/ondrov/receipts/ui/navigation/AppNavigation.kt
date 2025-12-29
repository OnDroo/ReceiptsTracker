package link.ondrov.receipts.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val animDuration = 400

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,

        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(animDuration)
            )
        },

        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(animDuration)
            ) + fadeOut(animationSpec = tween(animDuration))
        },

        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(animDuration)
            ) + fadeIn(animationSpec = tween(animDuration))
        },

        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(animDuration)
            )
        }
    ) {

        dashboardScreen(
            onOpenReceipts = { navController.navigate(Screen.ReceiptList.route) },
            onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetails.createRoute(id)) },
            onScanClick = { navController.navigate(Screen.Scan.route) },
            onSettingsClick = { navController.navigate(Screen.Settings.route) }
        )

        scanScreen(
            onScanFinished = { newId ->
                navController.popBackStack()
                navController.navigate(Screen.ReceiptDetails.createRoute(newId))
            },
            onScanCancelled = { navController.popBackStack() }
        )

        receiptListScreen(
            onBack = { navController.popBackStack() },
            onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetails.createRoute(id)) }
        )

        receiptDetailsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToEdit = { id -> navController.navigate(Screen.ReceiptEdit.createRoute(id)) }
        )

        receiptEditScreen(
            onDismiss = { navController.popBackStack() }
        )

        settingsScreen(
            onBack = { navController.popBackStack() }
        )
    }
}