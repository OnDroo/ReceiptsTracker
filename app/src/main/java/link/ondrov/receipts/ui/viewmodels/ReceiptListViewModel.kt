package link.ondrov.receipts.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.data.repository.ReceiptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the ReceiptListScreen and HomeScreen.
 * Provides the list of receipts from the repository.
 */
class ReceiptListViewModel(
    receiptRepository: ReceiptRepository
) : ViewModel() {

    // Expose the list of receipts as a StateFlow
    val receipts: StateFlow<List<ReceiptWithDetails>> = receiptRepository.getAllReceipts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 5-second stop timeout
            initialValue = emptyList()
        )
}