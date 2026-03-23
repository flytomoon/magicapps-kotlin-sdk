package com.magicapps.sdk

import com.magicapps.sdk.core.*
import com.magicapps.sdk.services.AuthService
import com.magicapps.sdk.services.GoogleAuthService
import com.magicapps.sdk.services.AiService
import com.magicapps.sdk.services.TemplatesService
import com.magicapps.sdk.services.DevicesService
import com.magicapps.sdk.services.EndpointsService
import com.magicapps.sdk.services.LookupTablesService
import com.magicapps.sdk.services.OwnerService
import com.magicapps.sdk.services.SettingsService
import com.magicapps.sdk.services.ProfileService
import com.magicapps.sdk.services.AccountService
import com.magicapps.sdk.services.FileStorageService
import com.magicapps.sdk.services.ConversationService
import com.magicapps.sdk.services.NotificationService
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Health check response from GET /ping.
 */
@Serializable
data class PingResponse(
    val message: String,
    val requestId: String? = null
)

/**
 * App info response from GET /apps/{appId}.
 */
@Serializable
data class AppInfo(
    @SerialName("app_id") val appId: String,
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val description: String? = null,
    val status: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val visibility: TemplateVisibility? = null
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
    /** Owner registration and migration service (all platforms). */
    val owner = OwnerService(http)
    /** Settings and configuration service (all platforms). */
    val settings = SettingsService(http)
    /** User profile service (all platforms). */
    val profile = ProfileService(http)
    /** Account management service (all platforms). */
    val account = AccountService(http)
    /** File storage service (all platforms). */
    val files = FileStorageService(http)
    /** AI conversations service (all platforms). */
    val conversations = ConversationService(http)
    /** Push notification registration service (all platforms). */
    val notifications = NotificationService(http)

    init {
        registry.register(auth)
        registry.register(googleAuth)
        registry.register(ai)
        registry.register(templates)
        registry.register(devices)
        registry.register(endpoints)
        registry.register(lookupTables)
        registry.register(owner)
        registry.register(settings)
        registry.register(profile)
        registry.register(account)
        registry.register(files)
        registry.register(conversations)
        registry.register(notifications)
    }

    /** Health check - verifies connectivity to the MagicApps API. */
    suspend fun ping(): PingResponse =
        http.get("/ping", authMode = AuthMode.NONE)

    /** Fetch app info for the current app_id. */
    suspend fun getAppInfo(): AppInfo =
        http.get("/apps/${http.appId}", authMode = AuthMode.NONE)

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
