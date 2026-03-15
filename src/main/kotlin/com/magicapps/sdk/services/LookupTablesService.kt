package com.magicapps.sdk.services

import com.magicapps.sdk.LookupTableDetail
import com.magicapps.sdk.LookupTableSummary
import com.magicapps.sdk.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- Lookup Table Types ---

@Serializable
data class LookupTableListResponse(
    val items: List<LookupTableSummary> = emptyList()
)

// --- Lookup Tables Service ---

/**
 * Lookup Tables service module.
 * Provides read-only access to lookup tables for client apps.
 * Lookup tables are created and managed in the tenant console;
 * client apps consume them via this SDK for reference data like
 * product catalogs, configuration lists, etc.
 * Available on all platforms.
 */
class LookupTablesService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "lookup-tables"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** List all available lookup tables for the current app. */
    suspend fun list(): LookupTableListResponse =
        http.get("/lookup-tables", authMode = AuthMode.OWNER)

    /** Get a specific lookup table's metadata by ID, including chunk references. */
    suspend fun get(lookupTableId: String): LookupTableDetail {
        val encoded = java.net.URLEncoder.encode(lookupTableId, "UTF-8")
        return http.get("/lookup-tables/$encoded", authMode = AuthMode.OWNER)
    }

    /**
     * Fetch an individual data chunk by index.
     *
     * @param lookupTableId The lookup table ID
     * @param chunkIndex Zero-based chunk index
     * @param version Optional version number for cache consistency
     */
    suspend fun getChunk(lookupTableId: String, chunkIndex: Int, version: Int? = null): JsonObject {
        val encoded = java.net.URLEncoder.encode(lookupTableId, "UTF-8")
        val query = if (version != null) mapOf("version" to version.toString()) else null
        return http.get("/lookup-tables/$encoded/chunks/$chunkIndex", query = query, authMode = AuthMode.OWNER)
    }

    /**
     * Convenience method that fetches all chunks for a table and assembles
     * the complete dataset by merging all chunk data objects.
     *
     * @param lookupTableId The lookup table ID
     * @return The complete dataset as a merged JsonObject
     */
    suspend fun getFullDataset(lookupTableId: String): JsonObject {
        val detail = get(lookupTableId)
        val result = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()

        for (i in 0 until (detail.chunkCount ?: 0)) {
            val chunk = getChunk(lookupTableId, i, detail.version)
            for ((key, value) in chunk) {
                result[key] = value
            }
        }

        return JsonObject(result)
    }
}
