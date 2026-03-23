package com.magicapps.sdk.services

import com.magicapps.sdk.AIConversation
import com.magicapps.sdk.AIConversationDetail
import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// --- Conversation Types ---

@Serializable
data class ConversationListResponse(
    val conversations: List<AIConversation> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null
)

@Serializable
data class ConversationDeleteResponse(
    val deleted: Boolean? = null,
    @SerialName("conversation_id") val conversationId: String? = null
)

@Serializable
data class AssistantMessage(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<JsonElement>? = null
)

@Serializable
data class ConversationUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null,
    @SerialName("estimated_cost_usd") val estimatedCostUsd: Double? = null
)

@Serializable
data class SendMessageResponse(
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("assistant_message") val assistantMessage: AssistantMessage? = null,
    val usage: ConversationUsage? = null,
    @SerialName("message_count") val messageCount: Int? = null
)

/**
 * AI Conversations service module.
 * Provides CRUD operations for AI conversations and message sending.
 * Conversations are scoped to the authenticated user and current app.
 * Available on all platforms.
 */
class ConversationService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "conversations"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Create a new AI conversation.
     *
     * @param title Optional conversation title (max 200 chars)
     * @param systemPrompt Optional system prompt (max 100000 chars)
     * @param metadata Optional arbitrary metadata
     */
    suspend fun createConversation(
        title: String? = null,
        systemPrompt: String? = null,
        metadata: Map<String, JsonElement>? = null
    ): AIConversation {
        val fields = mutableMapOf<String, JsonElement>()
        if (title != null) fields["title"] = kotlinx.serialization.json.JsonPrimitive(title)
        if (systemPrompt != null) fields["system_prompt"] = kotlinx.serialization.json.JsonPrimitive(systemPrompt)
        if (metadata != null) fields["metadata"] = JsonObject(metadata)
        val body = JsonObject(fields).toString()
        return http.post("/apps/${http.appId}/ai/conversations", body, AuthMode.BEARER)
    }

    /**
     * List conversations for the authenticated user.
     *
     * @param nextToken Pagination token from a previous response
     */
    suspend fun listConversations(nextToken: String? = null): ConversationListResponse {
        val query = if (nextToken != null) mapOf("next_token" to nextToken) else null
        return http.get("/apps/${http.appId}/ai/conversations", query = query, authMode = AuthMode.BEARER)
    }

    /**
     * Get a conversation with its message history.
     *
     * @param conversationId The conversation ID
     */
    suspend fun getConversation(conversationId: String): AIConversationDetail {
        val encoded = java.net.URLEncoder.encode(conversationId, "UTF-8")
        return http.get("/apps/${http.appId}/ai/conversations/$encoded", authMode = AuthMode.BEARER)
    }

    /**
     * Send a message in a conversation and receive the assistant's response.
     *
     * @param conversationId The conversation ID
     * @param content The message content (required)
     * @param stream Whether to stream the response (optional)
     * @param model Optional model override
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        stream: Boolean? = null,
        model: String? = null
    ): SendMessageResponse {
        val encoded = java.net.URLEncoder.encode(conversationId, "UTF-8")
        val fields = mutableMapOf<String, JsonElement>()
        fields["content"] = kotlinx.serialization.json.JsonPrimitive(content)
        if (stream != null) fields["stream"] = kotlinx.serialization.json.JsonPrimitive(stream)
        if (model != null) fields["model"] = kotlinx.serialization.json.JsonPrimitive(model)
        val body = JsonObject(fields).toString()
        return http.post("/apps/${http.appId}/ai/conversations/$encoded/messages", body, AuthMode.BEARER)
    }

    /**
     * Delete a conversation and all its messages.
     *
     * @param conversationId The conversation ID
     */
    suspend fun deleteConversation(conversationId: String): ConversationDeleteResponse {
        val encoded = java.net.URLEncoder.encode(conversationId, "UTF-8")
        return http.delete("/apps/${http.appId}/ai/conversations/$encoded", authMode = AuthMode.BEARER)
    }
}
