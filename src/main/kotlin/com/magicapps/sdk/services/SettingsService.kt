package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.json.JsonObject

/**
 * Settings service module.
 * Provides access to app settings, configuration, and integration secrets.
 * Available on all platforms.
 */
class SettingsService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "settings"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** Get the current app's settings. */
    suspend fun getSettings(): JsonObject =
        http.get("/apps/${http.appId}/settings", authMode = AuthMode.BEARER)

    /** Update the current app's settings. */
    suspend fun updateSettings(body: JsonObject): JsonObject =
        http.put("/apps/${http.appId}/settings", body.toString(), AuthMode.BEARER)

    /** Get the current app's configuration. */
    suspend fun getConfig(): JsonObject =
        http.get("/apps/${http.appId}/config", authMode = AuthMode.BEARER)

    /** Update the current app's configuration. */
    suspend fun updateConfig(body: JsonObject): JsonObject =
        http.put("/apps/${http.appId}/config", body.toString(), AuthMode.BEARER)

    /** Get the secret for a specific integration. */
    suspend fun getIntegrationSecret(integrationId: String): JsonObject {
        val encoded = java.net.URLEncoder.encode(integrationId, "UTF-8")
        return http.get("/apps/${http.appId}/integrations/$encoded/secret", authMode = AuthMode.BEARER)
    }

    /** Upload or update the secret for a specific integration. */
    suspend fun uploadIntegrationSecret(integrationId: String, body: JsonObject): JsonObject {
        val encoded = java.net.URLEncoder.encode(integrationId, "UTF-8")
        return http.post("/apps/${http.appId}/integrations/$encoded/secret", body.toString(), AuthMode.BEARER)
    }
}
