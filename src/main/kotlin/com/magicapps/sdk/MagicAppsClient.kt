package com.magicapps.sdk

import com.magicapps.sdk.core.*
import com.magicapps.sdk.services.AuthService
import kotlinx.serialization.Serializable

/**
 * Health check response from GET /ping.
 */
@Serializable
data class PingResponse(
    val message: String,
    val requestId: String? = null
)

/**
 * The main MagicApps SDK client for Android/Kotlin.
 *
 * Provides app_id-scoped API access with automatic authentication,
 * modular service plugins, and platform-conditional module availability.
 *
 * ```kotlin
 * val client = MagicAppsClient(SdkConfig(
 *     baseUrl = "https://api.magicapps.dev",
 *     appId = "my-app"
 * ))
 *
 * val pong = client.ping()
 * ```
 */
class MagicAppsClient(config: SdkConfig) {
    private val http = SdkHttpClient(config)
    private val registry = ServiceRegistry(SdkPlatform.ANDROID)

    /** Authentication service (all platforms). */
    val auth = AuthService(http)

    init {
        registry.register(auth)
    }

    /** Health check - verifies connectivity to the MagicApps API. */
    suspend fun ping(): PingResponse =
        http.get("/ping", authMode = AuthMode.NONE)

    /** Register a custom service module. */
    fun registerService(module: ServiceModule) {
        registry.register(module)
    }

    /** Get a service by name (platform-checked). */
    fun <T : ServiceModule> getService(name: String): T? =
        registry.get(name)

    /** Check if a service is available. */
    fun hasService(name: String): Boolean =
        registry.has(name)

    /** List all available services. */
    fun listServices(): List<ServiceModule> =
        registry.listAvailable()

    /** Update authentication tokens. */
    fun setTokens(accessToken: String? = null, refreshToken: String? = null, ownerToken: String? = null) {
        http.tokenManager.setTokens(accessToken, refreshToken, ownerToken)
    }

    /** Clear all authentication tokens. */
    fun clearTokens() {
        http.tokenManager.clearTokens()
    }
}
