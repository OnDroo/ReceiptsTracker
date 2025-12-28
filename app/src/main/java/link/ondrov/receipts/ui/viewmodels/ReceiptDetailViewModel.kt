package link.ondrov.receipts.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.model.Receipt
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptMetadata
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.data.repository.ReceiptRepository
import link.ondrov.receipts.data.repository.SettingsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class ReceiptDetailViewModel(
    application: Application,
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository,
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
            // 1. Delete File
            try {
                val file = File(getApplication<Application>().filesDir, current.imageFileName)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e("ReceiptVM", "Error deleting file", e)
            }
            // 2. Delete DB Entry
            receiptRepository.deleteReceipt(current)
            onComplete()
        }
    }

    fun updateReceipt(updated: ReceiptWithDetails, onComplete: () -> Unit) {
        viewModelScope.launch {
            receiptRepository.saveReceipt(updated.receipt, updated.items)
            onComplete()
        }
    }

    fun analyzeAndSave(imageFile: File, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                analyzeReceiptWithGemini(imageFile)
            } catch (e: Exception) {
                onError(e.message ?: "Unknown analysis error")
                // Cleanup on failure
                if (imageFile.exists()) imageFile.delete()
            }
        }
    }

    private suspend fun analyzeReceiptWithGemini(imageFile: File) = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.apiKey.first()

        if (apiKey.isBlank()) {
            throw IOException("API Key is missing. Please set it in Settings.")
        }

        val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=$apiKey"

        // 2. Fetch Categories for Enum
        val categories = receiptRepository.getAllCategories().first()
        val categoryNames = categories.joinToString(", ") { it.name }

        // Build JSON Array for Schema Enum
        val categoryNamesArray = JSONArray()
        categories.forEach { categoryNamesArray.put(it.name) }

        // 3. YOUR PROMPT (Restored)
        // We still include the list in text as a hint, but the Schema does the hard work.
        val prompt = """
            Analyze this receipt. Extract data to match the JSON structure exactly.
            
            RULES:
            1. Date must be in format "YYYY-MM-DD HH:mm:ss". If time is missing on receipt, use "00:00:00".
            2. Currency must be the 3-letter ISO 4217 code (e.g., EUR, USD, CZK, GBP). Infer from symbol or address.
            2. For 'items', price must be the TOTAL price for that line (qty * unit_price).
            3. Pick the single best category for this receipt from this list: [$categoryNames]. 
               Return the exact name in the 'categoryName' field.
        """.trimIndent()

        // 4. Encode Image
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val base64Image = bitmapToBase64(bitmap)

        // 5. Build JSON Request with STRICT SCHEMA
        val jsonSchema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("storeName", JSONObject().put("type", "STRING"))
                put("totalAmount", JSONObject().put("type", "NUMBER"))

                put("currency", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "ISO 4217 Code (e.g. EUR)")
                })

                put("date", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "YYYY-MM-DD HH:mm:ss")
                })

                // CRITICAL: The enum forces the model to pick a valid category
                put("categoryName", JSONObject().apply {
                    put("type", "STRING")
                    put("enum", categoryNamesArray)
                })

                put("items", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("name", JSONObject().put("type", "STRING"))
                            put("quantity", JSONObject().put("type", "NUMBER"))
                            put("unit", JSONObject().put("type", "STRING").put("nullable", true))
                            put("price", JSONObject().put("type", "NUMBER"))
                        })
                    })
                })
                put("metadata", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("paymentMethod", JSONObject().put("type", "STRING"))
                        put("transactionId", JSONObject().put("type", "STRING"))
                    })
                })
            })
            // CRITICAL: Required fields ensure they are not skipped
            put("required", JSONArray().apply {
                put("storeName")
                put("totalAmount")
                put("date")
                put("categoryName")
                put("items")
            })
        }

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg"); put("data", base64Image)
                    }))
                })
            }))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("responseSchema", jsonSchema)
            })
        }

        // 6. Send Request
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.write(payload.toString().toByteArray())

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val responseBody = connection.inputStream.bufferedReader().readText()
            val textData = JSONObject(responseBody).getJSONArray("candidates")
                .getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                .getJSONObject(0).getString("text")

            val json = JSONObject(textData)

            // Date Parsing
            val dateString = json.optString("date", "")
            val timestamp = try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(dateString)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }

            // Category Mapping
            val returnedCategoryName = json.optString("categoryName", "")
            val matchedCategory = categories.find { it.name.equals(returnedCategoryName, ignoreCase = true) }
            // Fallback to first category if matching somehow fails (though schema prevents this mostly)
            val categoryId = matchedCategory?.id ?: categories.firstOrNull()?.id

            // Map Items
            val id = imageFile.nameWithoutExtension
            val itemsList = (0 until json.getJSONArray("items").length()).map { i ->
                val item = json.getJSONArray("items").getJSONObject(i)
                ReceiptItem(
                    id = UUID.randomUUID().toString(),
                    receiptId = id,
                    name = item.getString("name"),
                    quantity = item.optDouble("quantity", 1.0),
                    unit = item.optString("unit", ""),
                    price = item.getDouble("price")
                )
            }

            val metaJson = json.optJSONObject("metadata")
            val newReceipt = Receipt(
                id = id,
                storeName = json.getString("storeName"),
                totalAmount = json.getDouble("totalAmount"),
                dateTimestamp = timestamp,
                currency = json.optString("currency", "EUR"),
                categoryId = categoryId,
                imageFileName = imageFile.name,
                metadata = ReceiptMetadata(metaJson?.optString("paymentMethod"), metaJson?.optString("transactionId"))
            )

            receiptRepository.saveReceipt(newReceipt, itemsList)
        } else {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: ""
            throw IOException("Gemini Error ${connection.responseCode}: $error")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as ReceiptApplication)
                val handle = createSavedStateHandle()
                ReceiptDetailViewModel(app,
                    app.container.receiptRepository,
                    app.container.settingsRepository,
                    handle
                )
            }
        }
    }
}