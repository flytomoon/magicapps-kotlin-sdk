package com.magicapps.sdk.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Manages JWT access tokens and owner tokens for the SDK.
 * Thread-safe with coroutine mutex for concurrent token refresh.
 * Tokens are persisted to the configured [TokenStorage] backend
 * (encrypted file storage by default) so they survive app restarts.
 */
class TokenManager(config: SdkConfig) {
    private var accessToken: String?
    private var refreshToken: String?
    private var ownerToken: String?
    private val baseUrl = config.baseUrl.trimEnd('/')
    private val onTokenRefresh = config.onTokenRefresh
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val storage: TokenStorage = config.tokenStorage

    init {
        // Load persisted tokens from storage, then override with any
        // tokens explicitly provided in the config.
        val storedAccess = try { storage.load(TokenStorageKeys.ACCESS_TOKEN) } catch (_: Exception) { null }
        val storedRefresh = try { storage.load(TokenStorageKeys.REFRESH_TOKEN) } catch (_: Exception) { null }
        val storedOwner = try { storage.load(TokenStorageKeys.OWNER_TOKEN) } catch (_: Exception) { null }

        accessToken = config.accessToken ?: storedAccess
        refreshToken = config.refreshToken ?: storedRefresh
        ownerToken = config.ownerToken ?: storedOwner

        // Persist any explicitly provided tokens so they are available next launch.
        config.accessToken?.let { try { storage.save(TokenStorageKeys.ACCESS_TOKEN, it) } catch (_: Exception) {} }
        config.refreshToken?.let { try { storage.save(TokenStorageKeys.REFRESH_TOKEN, it) } catch (_: Exception) {} }
        config.ownerToken?.let { try { storage.save(TokenStorageKeys.OWNER_TOKEN, it) } catch (_: Exception) {} }
    }

    /** Get the current access token, refreshing if expired. */
    suspend fun getAccessToken(): String? {
        val token = accessToken
        if (token != null && !isTokenExpired(token)) {
            return token
        }
        if (refreshToken != null) {
            return refreshAccessToken()
        }
        return accessToken
    }

    /** Get the owner token. */
    fun getOwnerToken(): String? = ownerToken

    /** Get the authorization header value for a given auth mode. */
    suspend fun getAuthHeader(mode: AuthMode): String? = when (mode) {
        AuthMode.BEARER -> getAccessToken()?.let { "Bearer $it" }
        AuthMode.OWNER -> getOwnerToken()?.let { "Bearer $it" }
        AuthMode.NONE -> null
    }

    /** Update stored tokens. Also persists to the configured storage backend. */
    fun setTokens(accessToken: String? = null, refreshToken: String? = null, ownerToken: String? = null) {
        accessToken?.let {
            this.accessToken = it
            try { storage.save(TokenStorageKeys.ACCESS_TOKEN, it) } catch (_: Exception) {}
        }
        refreshToken?.let {
            this.refreshToken = it
            try { storage.save(TokenStorageKeys.REFRESH_TOKEN, it) } catch (_: Exception) {}
        }
        ownerToken?.let {
            this.ownerToken = it
            try { storage.save(TokenStorageKeys.OWNER_TOKEN, it) } catch (_: Exception) {}
        }
    }

    /** Clear all stored tokens from memory and persistent storage. */
    fun clearTokens() {
        accessToken = null
        refreshToken = null
        ownerToken = null
        try { storage.deleteAll() } catch (_: Exception) {}
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true

            val payloadB64 = parts[1]
                .replace("-", "+")
                .replace("_", "/")
            val padded = payloadB64 + "=".repeat((4 - payloadB64.length % 4) % 4)
            val decoded = String(Base64.getDecoder().decode(padded))

            @Serializable
            data class JwtPayload(val exp: Long? = null)

            val payload = json.decodeFromString<JwtPayload>(decoded)
            val exp = payload.exp ?: return true

            // 30-second buffer
            System.currentTimeMillis() / 1000 > (exp - 30)
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun refreshAccessToken(): String? = mutex.withLock {
        // Double-check after acquiring lock
        val current = accessToken
        if (current != null && !isTokenExpired(current)) {
            return@withLock current
        }

        val rt = refreshToken ?: return@withLock null

        try {
            val url = java.net.URL("$baseUrl/auth/refresh")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.write("""{"refresh_token":"$rt"}""".toByteArray())

            if (connection.responseCode != 200) {
                accessToken = null
                refreshToken = null
                try { storage.delete(TokenStorageKeys.ACCESS_TOKEN) } catch (_: Exception) {}
                try { storage.delete(TokenStorageKeys.REFRESH_TOKEN) } catch (_: Exception) {}
                return@withLock null
            }

            @Serializable
            data class RefreshResponse(val accessToken: String, val refreshToken: String? = null)

            val responseBody = connection.inputStream.bufferedReader().readText()
            val tokens = json.decodeFromString<RefreshResponse>(responseBody)
            accessToken = tokens.accessToken
            try { storage.save(TokenStorageKeys.ACCESS_TOKEN, tokens.accessToken) } catch (_: Exception) {}
            tokens.refreshToken?.let {
                refreshToken = it
                try { storage.save(TokenStorageKeys.REFRESH_TOKEN, it) } catch (_: Exception) {}
            }

            onTokenRefresh?.invoke(TokenPair(tokens.accessToken, tokens.refreshToken))
            tokens.accessToken
        } catch (e: Exception) {
            accessToken
        }
    }
}
