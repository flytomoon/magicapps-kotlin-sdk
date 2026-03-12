package com.magicapps.sdk

import com.magicapps.sdk.core.*
import com.magicapps.sdk.services.AuthService
import com.magicapps.sdk.services.GoogleAuthService
import com.magicapps.sdk.services.AiService
import com.magicapps.sdk.services.TemplatesService
import com.magicapps.sdk.services.DevicesService
import com.magicapps.sdk.services.EndpointsService
import com.magicapps.sdk.services.LookupTablesService
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
 * The main Magic Apps Cloud SDK client for Android/Kotlin.
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
    /** Google Sign-In service (Android only). */
    val googleAuth = GoogleAuthService(http)
    /** AI proxy service (all platforms). */
    val ai = AiService(http)
    /** Templates service (all platforms). */
    val templates = TemplatesService(http)
    /** Devices catalog service (all platforms). */
    val devices = DevicesService(http)
    /** Endpoints and events service (all platforms). */
    val endpoints = EndpointsService(http)
    /** Lookup tables service (all platforms). */
    val lookupTables = LookupTablesService(http)

    init {
        registry.register(auth)
        registry.register(googleAuth)
        registry.register(ai)
        registry.register(templates)
        registry.register(devices)
        registry.register(endpoints)
        registry.register(lookupTables)
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

    /** Clear all authentication tokens from memory and persistent storage. */
    fun clearTokens() {
        http.tokenManager.clearTokens()
    }

    /** Convenience alias for [clearTokens] — clears all tokens on logout. */
    fun logout() {
        clearTokens()
    }
}
