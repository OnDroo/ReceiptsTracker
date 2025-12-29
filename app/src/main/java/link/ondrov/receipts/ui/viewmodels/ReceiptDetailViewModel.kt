package link.ondrov.receipts.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.data.repository.ReceiptRepository
import java.io.File

class ReceiptDetailViewModel(
    application: Application,
    private val receiptRepository: ReceiptRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val receiptId: String = checkNotNull(savedStateHandle["receiptId"])

    val uiState: StateFlow<ReceiptWithDetails?> = receiptRepository.getReceiptById(receiptId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun deleteReceipt(onComplete: () -> Unit) {
        val current = uiState.value?.receipt ?: return
        viewModelScope.launch {
            // 1. Delete the physical image file
            try {
                val file = File(getApplication<Application>().filesDir, current.imageFileName)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e("ReceiptVM", "Error deleting file", e)
            }
            // 2. Delete the DB Entry (Cascade deletes items)
            receiptRepository.deleteReceipt(current)
            onComplete()
        }
    }

    fun updateReceipt(updated: ReceiptWithDetails, onComplete: () -> Unit) {
        viewModelScope.launch {
            receiptRepository.updateReceipt(updated.receipt, updated.items)
            onComplete()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as ReceiptApplication)
                val handle = createSavedStateHandle()
                ReceiptDetailViewModel(
                    app,
                    app.container.receiptRepository,
                    handle
                )
            }
        }
    }
}