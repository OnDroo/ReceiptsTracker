package link.ondrov.receipts.ui.viewmodels

import androidx.lifecycle.ViewModel
import link.ondrov.receipts.data.repository.ReceiptRepository

/**
 * ViewModel for the SpendingReportScreen.
 * Will provide data for charts and spending summaries.
 */
class SpendingReportViewModel(
    receiptRepository: ReceiptRepository
) : ViewModel() {

    // TODO: Add logic to calculate spending totals from the repository.

    // For now, we'll just mock the monthly total.
    val totalSpentThisMonth: Double = 250.86
}