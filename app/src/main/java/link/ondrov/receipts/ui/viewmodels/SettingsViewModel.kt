package link.ondrov.receipts.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.repository.GeminiRepository
import link.ondrov.receipts.data.repository.SettingsRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository,
) : ViewModel() {

    // --- 1. EXISTING SETTINGS (Read directly from Repo) ---
    val apiKey: StateFlow<String> = settingsRepository.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    // --- 2. NEW AI MODEL SETTINGS ---

    // The list of models fetched from the API (Empty by default)
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    val selectedModel: StateFlow<String> = settingsRepository.selectedModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-4-hydrogen")


    // --- 3. ACTIONS ---

    fun updateApiKey(newKey: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(newKey)
            // Optional: Automatically fetch models when key is updated
            if (newKey.isNotBlank()) {
                fetchModels()
            }
        }
    }

    fun updateCurrency(newCurrency: String) {
        viewModelScope.launch {
            settingsRepository.saveCurrency(newCurrency)
        }
    }

    /**
     * Fetches the list of available models using the CURRENT API Key.
     * Called by the UI when the screen opens.
     */
    fun fetchModels() {
        viewModelScope.launch {
            val currentKey = apiKey.value
            if (currentKey.isNotBlank()) {
                val models = geminiRepository.fetchAvailableModels(currentKey)
                if (models.isNotEmpty()) {
                    _availableModels.value = models
                }
            }
        }
    }

    /**
     * Saves the user's chosen AI model to DataStore.
     */
    fun updateSelectedModel(newModel: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedModel(newModel)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as ReceiptApplication)
                SettingsViewModel(app.container.settingsRepository, app.container.geminiRepository)
            }
        }
    }
}