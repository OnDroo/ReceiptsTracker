package link.ondrov.receipts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelListResponse(
    val models: List<ModelInfo>
)

@Serializable
data class ModelInfo(
    val name: String, // e.g., "models/gemini-2.5-flash"
    val displayName: String?,
    val supportedGenerationMethods: List<String>?
)

@Serializable
data class GeminiErrorResponse(
    val error: GeminiErrorContent
)

@Serializable
data class GeminiErrorContent(
    val code: Int,
    val message: String,
    val status: String
)