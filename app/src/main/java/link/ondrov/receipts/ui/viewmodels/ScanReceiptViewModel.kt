package link.ondrov.receipts.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.repository.GeminiRepository
import org.json.JSONObject
import java.io.File

class ScanReceiptViewModel(
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    // --- UI STATE ---
    var isAnalyzing by mutableStateOf(false)
        private set

    // For the Error Dialog
    var showRetryDialog by mutableStateOf(false)
        private set
    var lastErrorMessage by mutableStateOf("")
        private set

    var isApiKeyMissingOrWrong by mutableStateOf(false)
        private set

    // Internal holder for the file in case we need to retry
    private var pendingImageFile: File? = null

    // Events to tell the UI to navigate
    private val _navigationEvents = Channel<String>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun analyzeImage(imageFile: File) {
        pendingImageFile = imageFile // Save for potential retry
        performAnalysis(imageFile)
    }

    private fun performAnalysis(file: File) {
        viewModelScope.launch {
            isAnalyzing = true
            try {
                val newReceiptId = geminiRepository.analyzeReceipt(file)

                pendingImageFile = null
                _navigationEvents.send(newReceiptId)

            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                isApiKeyMissingOrWrong = msg.contains("API key")

                lastErrorMessage = parseFriendlyErrorMessage(msg)
                showRetryDialog = true
            } finally {
                isAnalyzing = false
            }
        }
    }

    // --- RETRY LOGIC ---

    fun retryAnalysis() {
        showRetryDialog = false
        pendingImageFile?.let { performAnalysis(it) }
    }

    fun dismissError() {
        // User gave up, delete the temp file
        pendingImageFile?.delete()
        pendingImageFile = null
        showRetryDialog = false
    }


    private fun parseFriendlyErrorMessage(rawMessage: String): String {
        // 1. Check for common HTTP codes first
        if (rawMessage.contains("429") || rawMessage.contains("RESOURCE_EXHAUSTED")) {
            return "Quota Exceeded. You are using the Free Tier and have hit the rate limit.\n\nPlease wait a moment or switch to the 'Flash' model (it has higher limits)."
        }
        if (rawMessage.contains("503")) {
            return "Gemini Service is overloaded. Please try again in a few seconds."
        }

        // 2. Try to extract the actual "message" field from the JSON
        // The error usually looks like: "Gemini Failed (400): { "error": { "message": "..." } }"
        try {
            val jsonStartIndex = rawMessage.indexOf("{")
            if (jsonStartIndex != -1) {
                val jsonStr = rawMessage.substring(jsonStartIndex)
                val jsonObj = JSONObject(jsonStr)

                // Navigate: error -> message
                if (jsonObj.has("error")) {
                    val errorContent = jsonObj.optJSONObject("error")
                    val actualMsg = errorContent?.optString("message")
                    if (!actualMsg.isNullOrBlank()) {
                        return actualMsg
                    }
                }
            }
        } catch (e: Exception) {
            // JSON parsing failed, just fall through
        }

        // 3. Fallback: Return truncated message if it's too huge
        return if (rawMessage.length > 200) rawMessage.take(200) + "..." else rawMessage
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as ReceiptApplication)
                ScanReceiptViewModel(app.container.geminiRepository)
            }
        }
    }
}