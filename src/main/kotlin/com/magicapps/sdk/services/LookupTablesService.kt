package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- Lookup Table Types ---

@Serializable
data class LookupTableSummary(
    @SerialName("lookup_table_id") val lookupTableId: String,
    val name: String,
    val description: String? = null,
    @SerialName("schema_keys") val schemaKeys: List<String> = emptyList(),
    @SerialName("schema_key_count") val schemaKeyCount: Int = 0,
    @SerialName("schema_keys_truncated") val schemaKeysTruncated: Boolean = false,
    val version: Int = 1,
    @SerialName("payload_hash") val payloadHash: String = "",
    @SerialName("storage_mode") val storageMode: String = "chunked",
    @SerialName("chunk_count") val chunkCount: Int = 0,
    @SerialName("updated_at") val updatedAt: Long = 0
)

@Serializable
data class LookupTableListResponse(
    val items: List<LookupTableSummary> = emptyList()
)

@Serializable
data class LookupTableChunkRef(
    val index: Int,
    val path: String,
    val sha256: String = "",
    @SerialName("byte_length") val byteLength: Int = 0
)

@Serializable
data class LookupTableDetail(
    @SerialName("lookup_table_id") val lookupTableId: String,
    val name: String,
    val description: String? = null,
    @SerialName("schema_keys") val schemaKeys: List<String> = emptyList(),
    @SerialName("schema_key_count") val schemaKeyCount: Int = 0,
    @SerialName("schema_keys_truncated") val schemaKeysTruncated: Boolean = false,
    val version: Int = 1,
    @SerialName("payload_hash") val payloadHash: String = "",
    @SerialName("storage_mode") val storageMode: String = "chunked",
    @SerialName("chunk_count") val chunkCount: Int = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    val prompt: String? = null,
    @SerialName("default_success_sentence") val defaultSuccessSentence: String? = null,
    @SerialName("default_fail_sentence") val defaultFailSentence: String? = null,
    @SerialName("chunk_encoding") val chunkEncoding: String = "json",
    @SerialName("manifest_hash") val manifestHash: String = "",
    val chunks: List<LookupTableChunkRef> = emptyList()
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

        for (i in 0 until detail.chunkCount) {
            val chunk = getChunk(lookupTableId, i, detail.version)
            for ((key, value) in chunk) {
                result[key] = value
            }
        }

        return JsonObject(result)
    }
}
