package com.magicapps.sdk.core

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

/**
 * Internal HTTP client for the MagicApps SDK.
 * Wraps HttpURLConnection with app_id scoping, authentication, retries, and typed errors.
 */
class SdkHttpClient(config: SdkConfig) {
    val tokenManager = TokenManager(config)
    private val baseUrl = config.baseUrl.trimEnd('/')
    internal val appId = config.appId
    private val defaultRetries = config.retries
    private val retryDelayMs = config.retryDelayMs
    private val json = Json { ignoreUnknownKeys = true }

    /** Make a GET request. */
    suspend inline fun <reified T> get(
        path: String,
        query: Map<String, String>? = null,
        authMode: AuthMode = AuthMode.BEARER,
        retries: Int? = null
    ): T = request("GET", path, null, query, authMode, retries)

    /** Make a POST request. */
    suspend inline fun <reified T> post(
        path: String,
        body: String? = null,
        authMode: AuthMode = AuthMode.BEARER,
        retries: Int? = null
    ): T = request("POST", path, body, null, authMode, retries)

    /** Make a PUT request. */
    suspend inline fun <reified T> put(
        path: String,
        body: String? = null,
        authMode: AuthMode = AuthMode.BEARER,
        retries: Int? = null
    ): T = request("PUT", path, body, null, authMode, retries)

    /** Make a DELETE request. */
    suspend inline fun <reified T> delete(
        path: String,
        authMode: AuthMode = AuthMode.BEARER,
        retries: Int? = null
    ): T = request("DELETE", path, null, null, authMode, retries)

    @PublishedApi
    internal suspend inline fun <reified T> request(
        method: String,
        path: String,
        body: String?,
        query: Map<String, String>?,
        authMode: AuthMode,
        retries: Int?
    ): T {
        val maxRetries = retries ?: defaultRetries
        val url = buildUrl(path, query)

        var attempt = 0
        while (true) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-App-Id", appId)

                tokenManager.getAuthHeader(authMode)?.let {
                    connection.setRequestProperty("Authorization", it)
                }

                if (body != null) {
                    connection.doOutput = true
                    connection.outputStream.write(body.toByteArray())
                }

                val statusCode = connection.responseCode

                if (statusCode in 200..299) {
                    if (statusCode == 204) {
                        return json.decodeFromString("{}")
                    }
                    val responseBody = connection.inputStream.bufferedReader().readText()
                    return json.decodeFromString(responseBody)
                }

                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText()
                } catch (_: Exception) {
                    null
                }

                val payload = errorBody?.let {
                    try { json.decodeFromString<ApiErrorPayload>(it) } catch (_: Exception) { null }
                }

                val isIdempotent = method == "GET"
                val isRetryable = statusCode >= 500 || statusCode == 429

                if (attempt < maxRetries && isIdempotent && isRetryable) {
                    val backoff = retryDelayMs * 2.0.pow(attempt.toDouble()).toLong()
                    attempt++
                    delay(backoff)
                    continue
                }

                throw createApiException(statusCode, payload)

            } catch (e: ApiException) {
                throw e
            } catch (e: Exception) {
                val isIdempotent = method == "GET"
                if (attempt < maxRetries && isIdempotent) {
                    val backoff = retryDelayMs * 2.0.pow(attempt.toDouble()).toLong()
                    attempt++
                    delay(backoff)
                    continue
                }
                throw NetworkException("Network request failed", e)
            }
        }
    }

    @PublishedApi
    internal fun buildUrl(path: String, query: Map<String, String>?): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val urlStr = "$baseUrl$normalizedPath"

        if (query.isNullOrEmpty()) return urlStr

        val queryString = query.entries
            .filter { it.value.isNotBlank() }
            .joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }

        return if (queryString.isNotEmpty()) "$urlStr?$queryString" else urlStr
    }
}
