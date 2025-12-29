package link.ondrov.receipts.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Scan : Screen("scan")
    data object Settings : Screen("settings")
    data object ReceiptList : Screen("receipt_list")

    data object ReceiptDetails : Screen("receipt_detail/{receiptId}") {
        fun createRoute(receiptId: String) = "receipt_detail/$receiptId"
    }

    data object ReceiptEdit : Screen("receipt_edit/{receiptId}") {
        fun createRoute(receiptId: String) = "receipt_edit/$receiptId"
    }
}