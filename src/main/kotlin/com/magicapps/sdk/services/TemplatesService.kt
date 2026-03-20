package com.magicapps.sdk.services

import com.magicapps.sdk.Template
import com.magicapps.sdk.core.*
import kotlinx.serialization.json.JsonObject

/**
 * Templates service module.
 * Provides read-only access to the template catalog for the current app.
 * Available on all platforms.
 */
class TemplatesService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "templates"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** Get a specific template by ID. */
    suspend fun get(templateId: String): Template =
        http.get("/apps/${http.appId}/templates/$templateId", authMode = AuthMode.NONE)

    /** Fetch the full catalog for the current app. */
    suspend fun getCatalog(): JsonObject =
        http.get("/apps/${http.appId}/catalog", authMode = AuthMode.NONE)
}
