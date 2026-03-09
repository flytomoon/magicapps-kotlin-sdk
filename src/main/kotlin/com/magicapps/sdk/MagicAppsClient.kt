package com.magicapps.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for the MagicApps SDK client.
 */
data class MagicAppsConfig(
    /** Base URL of the MagicApps API. */
    val baseUrl: String,
    /** The app_id for your registered application. */
    val appId: String,
    /** Optional auth token for authenticated requests. */
    val authToken: String? = null,
    /** Request timeout in milliseconds. Defaults to 30000. */
    val timeout: Long = 30_000L
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(appId.isNotBlank()) { "appId must not be blank" }
    }
}

/** Information about a registered application. */
@Serializable
data class AppInfo(
    @SerialName("app_id") val appId: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

/** A template within an application. */
@Serializable
data class Template(
    @SerialName("template_id") val templateId: String,
    @SerialName("app_id") val appId: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

/** Base exception for MagicApps SDK errors. */
open class MagicAppsException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Exception from the MagicApps API. */
class ApiException(
    val statusCode: Int,
    message: String,
    val responseBody: String? = null
) : MagicAppsException("API error $statusCode: $message")

/**
 * MagicApps API client for Kotlin/JVM applications.
 */
class MagicAppsClient(private val config: MagicAppsConfig) {

    private val normalizedBaseUrl = config.baseUrl.trimEnd('/')

    /** Get information about the current application. */
    suspend fun getAppInfo(): AppInfo {
        return request("GET", "/apps/${config.appId}")
    }

    /** List templates for the current application. */
    suspend fun listTemplates(nextToken: String? = null): List<Template> {
        val query = if (nextToken != null) "?next_token=$nextToken" else ""
        return request("GET", "/apps/${config.appId}/templates$query")
    }

    /** Get a specific template by ID. */
    suspend fun getTemplate(id: String): Template {
        return request("GET", "/apps/${config.appId}/templates/$id")
    }

    private suspend inline fun <reified T> request(
        method: String,
        path: String
    ): T {
        // Implementation uses Ktor client - placeholder for SDK scaffold
        // Full HTTP implementation will be added when SDK is production-ready
        throw NotImplementedError("HTTP client integration pending - see SDK roadmap")
    }
}
