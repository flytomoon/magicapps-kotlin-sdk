package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Chat Completion Types ---

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val messages: List<ChatMessage>,
    val model: String? = null,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    val stop: List<String>? = null
)

@Serializable
data class ChatCompletionChoice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class TokenUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Int? = null,
    val model: String? = null,
    val choices: List<ChatCompletionChoice>,
    val usage: TokenUsage? = null
)

// --- Embedding Types ---

@Serializable
data class EmbeddingData(
    val `object`: String? = null,
    val embedding: List<Double>,
    val index: Int
)

@Serializable
data class EmbeddingResponse(
    val `object`: String? = null,
    val data: List<EmbeddingData>,
    val model: String? = null,
    val usage: TokenUsage? = null
)

// --- Image Generation Types ---

@Serializable
data class GeneratedImage(
    val url: String? = null,
    @SerialName("b64_json") val b64Json: String? = null,
    @SerialName("revised_prompt") val revisedPrompt: String? = null
)

@Serializable
data class ImageGenerationResponse(
    val created: Int? = null,
    val data: List<GeneratedImage>
)

// --- Content Moderation Types ---

@Serializable
data class ModerationCategories(
    val hate: Boolean? = null,
    val sexual: Boolean? = null,
    val violence: Boolean? = null,
    @SerialName("self-harm") val selfHarm: Boolean? = null,
    val harassment: Boolean? = null
)

@Serializable
data class ModerationCategoryScores(
    val hate: Double? = null,
    val sexual: Double? = null,
    val violence: Double? = null,
    @SerialName("self-harm") val selfHarm: Double? = null,
    val harassment: Double? = null
)

@Serializable
data class ModerationResult(
    val flagged: Boolean,
    val categories: ModerationCategories? = null,
    @SerialName("category_scores") val categoryScores: ModerationCategoryScores? = null
)

@Serializable
data class ModerationResponse(
    val id: String? = null,
    val model: String? = null,
    val results: List<ModerationResult>
)

// --- AI Usage Types ---

/// A single period summary record from the AI usage summary endpoint.
/// Source: lambda/ai_proxy/index.js handleGetUsageSummary (~line 457-474)
/// Fields from DynamoDB AI_USAGE_SUMMARY_TABLE.
@Serializable
data class AiUsageSummaryRecord(
    @SerialName("app_id") val appId: String? = null,
    val period: String? = null,
    @SerialName("total_requests") val totalRequests: Int? = null,
    @SerialName("total_input_tokens") val totalInputTokens: Int? = null,
    @SerialName("total_output_tokens") val totalOutputTokens: Int? = null,
    @SerialName("total_estimated_cost_usd") val totalEstimatedCostUsd: Double? = null,
    @SerialName("updated_at") val updatedAt: Double? = null
)

/// Response wrapper from GET /apps/{app_id}/ai/usage/summary.
/// Source: lambda/ai_proxy/index.js handleGetUsageSummary (~line 457-474)
/// Response shape: { summaries: AiUsageSummaryRecord[] }
@Serializable
data class AiUsageSummary(
    val summaries: List<AiUsageSummaryRecord>? = null
) {
    /** Convenience: get the total requests across all summary records. */
    val totalRequests: Int?
        get() = summaries?.sumOf { it.totalRequests ?: 0 }
}

// --- AI Detailed Usage Types ---

/** A single AI usage record (per-request detail). */
@Serializable
data class AiUsageRecord(
    @SerialName("usage_id") val usageId: String,
    @SerialName("app_id") val appId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("model_id") val modelId: String,
    @SerialName("request_type") val requestType: String,
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
    @SerialName("latency_ms") val latencyMs: Int,
    val status: String,
    @SerialName("created_at") val createdAt: Double,
    @SerialName("expires_at") val expiresAt: Double,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("user_id") val userId: String? = null
)

/** Response from the detailed AI usage endpoint. */
@Serializable
data class AiUsageResponse(
    val usage: List<AiUsageRecord>,
    val count: Int
)

/**
 * AI proxy service module.
 * Provides access to chat completions, embeddings, image generation,
 * and content moderation via the platform's AI proxy.
 * The proxy routes requests through the tenant's configured providers
 * (OpenAI, Anthropic, Google) so developers don't manage API keys on the client.
 * Available on all platforms.
 */
class AiService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "ai"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = false }

    // --- Chat Completions ---

    /** Create a chat completion via the AI proxy. */
    suspend fun createChatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        val body = json.encodeToString(ChatCompletionRequest.serializer(), request)
        return http.post("/apps/${http.appId}/ai/chat/completions", body)
    }

    /** Convenience: create a simple chat completion with a single user message. */
    suspend fun chat(message: String, model: String? = null): ChatCompletionResponse {
        val request = ChatCompletionRequest(
            messages = listOf(ChatMessage(role = "user", content = message)),
            model = model
        )
        return createChatCompletion(request)
    }

    // --- Embeddings ---

    /** Generate embeddings for the given input text. */
    suspend fun createEmbedding(input: String, model: String? = null): EmbeddingResponse {
        val modelField = if (model != null) ""","model":"$model"""" else ""
        val body = """{"input":"$input"$modelField}"""
        return http.post("/apps/${http.appId}/ai/embeddings", body)
    }

    // --- Image Generation ---

    /** Generate images from a text prompt. */
    suspend fun createImage(prompt: String, n: Int? = null, size: String? = null, model: String? = null): ImageGenerationResponse {
        val fields = mutableListOf(""""prompt":"$prompt"""")
        if (n != null) fields.add(""""n":$n""")
        if (size != null) fields.add(""""size":"$size"""")
        if (model != null) fields.add(""""model":"$model"""")
        val body = "{${fields.joinToString(",")}}"
        return http.post("/apps/${http.appId}/ai/images/generations", body)
    }

    // --- Content Moderation ---

    /** Check content for policy violations. */
    suspend fun createModeration(input: String, model: String? = null): ModerationResponse {
        val modelField = if (model != null) ""","model":"$model"""" else ""
        val body = """{"input":"$input"$modelField}"""
        return http.post("/apps/${http.appId}/ai/moderations", body)
    }

    // --- Usage ---

    /**
     * Get detailed per-request AI usage records for the current app.
     *
     * @param limit Maximum number of records to return (1-100, default 50).
     * @param startDate Start of date range filter (ISO 8601 string).
     * @param endDate End of date range filter (ISO 8601 string).
     * @return A response containing individual usage records and count.
     */
    suspend fun getUsage(limit: Int? = null, startDate: String? = null, endDate: String? = null): AiUsageResponse {
        val query = mutableMapOf<String, String>()
        if (limit != null) query["limit"] = limit.toString()
        if (startDate != null) query["start_date"] = startDate
        if (endDate != null) query["end_date"] = endDate
        return http.get("/apps/${http.appId}/ai/usage", query = query.ifEmpty { null })
    }

    /** Get AI usage summary for the current app. */
    suspend fun getUsageSummary(): AiUsageSummary =
        http.get("/apps/${http.appId}/ai/usage/summary")
}
