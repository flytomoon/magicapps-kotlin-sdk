package com.magicapps.sdk.services

import com.magicapps.sdk.Template
import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Template Types ---

@Serializable
data class TemplateListResponse(
    val templates: List<Template>? = null,
    val items: List<Template>? = null,
    @SerialName("next_token") val nextToken: String? = null,
    val count: Int? = null
) {
    /** Convenience accessor returning templates from whichever field the API uses. */
    val allTemplates: List<Template> get() = templates ?: items ?: emptyList()
}

@Serializable
data class RegistryApp(
    @SerialName("app_id") val appId: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    val status: String? = null,
    val visibility: String? = null
)

@Serializable
data class RegistryAppsResponse(
    val apps: List<RegistryApp>? = null,
    val items: List<RegistryApp>? = null
) {
    /** Convenience accessor returning apps from whichever field the API uses. */
    val allApps: List<RegistryApp> get() = apps ?: items ?: emptyList()
}

/**
 * Templates service module.
 * Provides CRUD operations for templates within a tenant's app,
 * plus read access to the registry catalog.
 * Available on all platforms.
 */
class TemplatesService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "templates"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** List templates for the current app. */
    suspend fun list(nextToken: String? = null): TemplateListResponse {
        val query = if (nextToken != null) mapOf("next_token" to nextToken) else null
        return http.get("/apps/${http.appId}/templates", query = query, authMode = AuthMode.NONE)
    }

    /** Get a specific template by ID. */
    suspend fun get(templateId: String): Template =
        http.get("/apps/${http.appId}/templates/$templateId", authMode = AuthMode.NONE)

    /** Create a new template for the current app. Requires authentication. */
    suspend fun create(name: String, description: String? = null, content: String? = null): Template {
        val descField = if (description != null) ""","description":"$description"""" else ""
        val contentField = if (content != null) ""","content":$content""" else ""
        val body = """{"name":"$name"$descField$contentField}"""
        return http.post("/apps/${http.appId}/templates", body)
    }

    /** Update an existing template. Requires authentication. */
    suspend fun update(templateId: String, name: String? = null, description: String? = null, content: String? = null): Template {
        val fields = mutableListOf<String>()
        if (name != null) fields.add(""""name":"$name"""")
        if (description != null) fields.add(""""description":"$description"""")
        if (content != null) fields.add(""""content":$content""")
        val body = "{${fields.joinToString(",")}}"
        return http.put("/apps/${http.appId}/templates/$templateId", body)
    }

    /** Delete a template. Requires authentication. */
    suspend fun delete(templateId: String) {
        http.delete<Unit>("/apps/${http.appId}/templates/$templateId")
    }

    /** Browse the registry catalog of well-known apps and templates. */
    suspend fun browseRegistry(): RegistryAppsResponse =
        http.get("/registry/apps", authMode = AuthMode.NONE)
}
