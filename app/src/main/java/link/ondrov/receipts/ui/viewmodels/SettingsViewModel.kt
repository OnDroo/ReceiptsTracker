package link.ondrov.receipts.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.repository.SettingsRepository

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    // Read directly from Repo
    val apiKey: StateFlow<String> = repository.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val currency: StateFlow<String> = repository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    fun updateApiKey(newKey: String) {
        viewModelScope.launch {
            repository.saveApiKey(newKey)
        }
    }

    fun updateCurrency(newCurrency: String) {
        viewModelScope.launch {
            repository.saveCurrency(newCurrency)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as ReceiptApplication)
                SettingsViewModel(app.container.settingsRepository)
            }
        }
    }
}