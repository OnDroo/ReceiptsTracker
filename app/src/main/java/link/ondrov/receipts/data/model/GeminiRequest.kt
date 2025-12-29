package link.ondrov.receipts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// --- 1. THE REQUEST ---
@Serializable
data class GeminiGenerateRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String // Base64 string
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String,
    val responseSchema: JsonElement // We can pass the raw JSON object here
)

// --- 2. THE RESPONSE ---
@Serializable
data class GeminiGenerateResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

// --- 3. THE PARSED RESULT (What the AI returns inside the 'text' field) ---
@Serializable
data class ReceiptAnalysisResult(
    val storeName: String,
    val totalAmount: Double,
    val currency: String = "EUR",
    val date: String,
    val categoryName: String? = null,
    val items: List<AnalysisItem>,
    val metadata: AnalysisMetadata? = null
)

@Serializable
data class AnalysisItem(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val price: Double
)

@Serializable
data class AnalysisMetadata(
    val paymentMethod: String? = null,
    val transactionId: String? = null
)