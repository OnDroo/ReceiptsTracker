package link.ondrov.receipts.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
    data object ReceiptList : Screen("receipt_list")

    object ReceiptDetails {
        const val route = "receipt_detail/{receiptId}?imageUri={imageUri}"

        fun createRoute(receiptId: String, imageUri: String? = null): String {
            return if (imageUri != null) {
                "receipt_detail/$receiptId?imageUri=$imageUri"
            } else {
                "receipt_detail/$receiptId"
            }
        }
    }

    object ReceiptEdit {
        const val route = "receipt_edit/{receiptId}"
        fun createRoute(receiptId: String) = "receipt_edit/$receiptId"
    }
}