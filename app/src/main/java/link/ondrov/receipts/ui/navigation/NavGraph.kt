package link.ondrov.receipts.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import link.ondrov.receipts.ui.screens.*
import link.ondrov.receipts.ui.viewmodels.ReceiptDetailViewModel

fun NavGraphBuilder.dashboardScreen(
    onOpenReceipts: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    composable(Screen.Dashboard.route) {
        DashboardRoute(
            onOpenReceipts = onOpenReceipts,
            onReceiptClick = onReceiptClick,
            onScanClick = onScanClick,
            onSettingsClick = onSettingsClick
        )
    }
}

fun NavGraphBuilder.scanScreen(
    onScanFinished: (String) -> Unit,
    onScanCancelled: () -> Unit
) {
    composable(Screen.Scan.route) {
        ScanReceiptRoute(
            onScanFinished = onScanFinished,
            onScanCancelled = onScanCancelled
        )
    }
}

fun NavGraphBuilder.receiptListScreen(
    onBack: () -> Unit,
    onReceiptClick: (String) -> Unit
) {
    composable(Screen.ReceiptList.route) {
        ReceiptListRoute(
            onBack = onBack,
            onReceiptClick = onReceiptClick
        )
    }
}

fun NavGraphBuilder.receiptDetailsScreen(
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    composable(
        route = Screen.ReceiptDetails.route,
        arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
    ) { backStackEntry ->
        val receiptId = backStackEntry.arguments?.getString("receiptId") ?: return@composable

        ReceiptDetailScreen(
            receiptId = receiptId,
            onBack = onBack,
            onNavigateToEdit = onNavigateToEdit
        )
    }
}

fun NavGraphBuilder.receiptEditScreen(
    onDismiss: () -> Unit
) {
    composable(
        route = Screen.ReceiptEdit.route,
        arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
    ) {
        val viewModel: ReceiptDetailViewModel = viewModel(factory = ReceiptDetailViewModel.Factory)
        val uiState by viewModel.uiState.collectAsState()

        uiState?.let { receipt ->
            EditReceiptScreen(
                originalReceipt = receipt,
                onDismiss = onDismiss,
                onSave = { updatedReceipt ->
                    viewModel.updateReceipt(updatedReceipt) {
                        onDismiss()
                    }
                }
            )
        }
    }
}

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit
) {
    composable(Screen.Settings.route) {
        SettingsScreen(onBack = onBack)
    }
}