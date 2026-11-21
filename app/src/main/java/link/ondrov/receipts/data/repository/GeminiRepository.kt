package link.ondrov.receipts.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import link.ondrov.receipts.data.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale
import java.util.UUID

class GeminiRepository(
    private val settingsRepository: SettingsRepository,
    private val receiptRepository: ReceiptRepository
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(jsonParser)
        }
    }

    /**
     * Fetches available models from Gemini API.
     * Used by Settings Screen.
     */
    suspend fun fetchAvailableModels(apiKey: String): List<String> {
        try {
            val response: ModelListResponse = httpClient.get(
                "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            ).body()

            return response.models
                .filter { model ->
                    val name = model.name.lowercase()
                    val isGenerative = model.supportedGenerationMethods?.contains("generateContent") == true
                    val isGemini = name.contains("gemini")
                    val isFreeTierEligible = name.contains("flash") || name.contains("pro") || name.contains("hydrogen")
                    isGenerative && isGemini && isFreeTierEligible
                }
                .map { it.name.removePrefix("models/") }
                .sortedWith(
                    compareByDescending<String> { it.contains("hydrogen") }
                        .thenByDescending { it.contains("flash") }
                        .thenByDescending { it }
                )
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Analyzes a receipt image, extracts data, matches categories, and saves to DB.
     */
    suspend fun analyzeReceipt(imageFile: File) : String = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.apiKey.first()
        if (apiKey.isBlank()) throw IOException("API key is missing. Please check Settings.")

        val modelName = settingsRepository.selectedModel.first()

        val categories = receiptRepository.getAllCategories().first()
        val categoryNames = categories.map { it.name }

        val prompt = """
            Analyze this receipt. Extract data to match the JSON structure exactly.
            RULES:
            1. Date must be in format "YYYY-MM-DD HH:mm:ss". If time is missing on receipt, use "00:00:00".
            2. Currency must be the 3-letter ISO 4217 code (e.g., EUR, USD, CZK, GBP). Infer from symbol or address.
            3. For 'items', price must be the TOTAL price for that line (qty * unit_price).
            4. Pick the single best category for this receipt from this list: [$categoryNames]. 
               Return the exact name in the 'categoryName' field.
        """.trimIndent()

        // 3. Process Image
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val base64Image = bitmapToBase64(bitmap)

        // 4. Define Strict JSON Schema for Gemini
        val jsonSchema = buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("storeName", buildJsonObject { put("type", "STRING") })
                put("totalAmount", buildJsonObject { put("type", "NUMBER") })
                put("currency", buildJsonObject { put("type", "STRING"); put("description", "ISO Code") })
                put("date", buildJsonObject { put("type", "STRING"); put("description", "YYYY-MM-DD HH:mm:ss") })
                put("categoryName", buildJsonObject {
                    put("type", "STRING")
                    put("enum", JsonArray(categoryNames.map { JsonPrimitive(it) }))
                })
                put("items", buildJsonObject {
                    put("type", "ARRAY")
                    put("items", buildJsonObject {
                        put("type", "OBJECT")
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "STRING") })
                            put("quantity", buildJsonObject { put("type", "NUMBER") })
                            put("unit", buildJsonObject { put("type", "STRING"); put("nullable", true) })
                            put("price", buildJsonObject { put("type", "NUMBER") })
                        })
                    })
                })
                put("metadata", buildJsonObject {
                    put("type", "OBJECT")
                    put("properties", buildJsonObject {
                        put("paymentMethod", buildJsonObject { put("type", "STRING") })
                        put("transactionId", buildJsonObject { put("type", "STRING") })
                    })
                })
            })
            put("required", JsonArray(listOf("storeName", "totalAmount", "date", "items").map { JsonPrimitive(it) }))
        }

        // 5. Build Request
        val requestPayload = GeminiGenerateRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(text = prompt),
                    Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                ))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = jsonSchema
            )
        )

        // 6. Execute Network Call
        val httpResponse: HttpResponse = httpClient.post(
            "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestPayload)
        }

        if (httpResponse.status.isSuccess()) {
            // --- SUCCESS PATH ---
            val response: GeminiGenerateResponse = httpResponse.body()

            val rawJsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IOException("Gemini returned an empty response (No text generated).")

            // Parse the Inner JSON string (the receipt data)
            val result = try {
                jsonParser.decodeFromString<ReceiptAnalysisResult>(rawJsonText)
            } catch (e: Exception) {
                throw IOException("Failed to parse receipt JSON from AI: ${e.message}")
            }

            // Date Parsing
            val timestamp = parseDateString(result.date)

            // Category Matching
            val matchedCategory = categories.find { it.name.equals(result.categoryName, ignoreCase = true) }
            val categoryId = matchedCategory?.id // If null, it's "Uncategorized"

            val receiptId = imageFile.nameWithoutExtension

            // Map Items
            val itemsList = result.items.map { item ->
                ReceiptItem(
                    id = UUID.randomUUID().toString(),
                    receiptId = receiptId,
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    price = item.price
                )
            }

            // Create Receipt
            val newReceipt = Receipt(
                id = receiptId,
                storeName = result.storeName,
                totalAmount = result.totalAmount,
                dateTimestamp = timestamp,
                currency = result.currency,
                categoryId = categoryId,
                imageFileName = imageFile.name,
                metadata = ReceiptMetadata(result.metadata?.paymentMethod, result.metadata?.transactionId)
            )

            // 8. Save to DB
            receiptRepository.saveReceipt(newReceipt, itemsList)

            return@withContext receiptId

        } else {
            // --- ERROR PATH (e.g., 429 Quota Exceeded, 503, 400) ---
            val errorText = httpResponse.bodyAsText()
            try {
                // Try to parse the specific Gemini Error format
                val errorObj = jsonParser.decodeFromString<GeminiErrorResponse>(errorText)
                throw IOException("Gemini Error ${errorObj.error.code}: ${errorObj.error.message}")
            } catch (_: Exception) {
                // If the error format is unknown, just return the raw text + status code
                throw IOException("Gemini Failed (${httpResponse.status.value}): $errorText")
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    private fun parseDateString(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(dateString)?.time
        } catch (_: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)?.time
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()
    }
}