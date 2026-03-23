package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- File Storage Types ---

@Serializable
data class UploadUrlResponse(
    @SerialName("file_id") val fileId: String? = null,
    @SerialName("upload_url") val uploadUrl: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null
)

@Serializable
data class FileMetadata(
    @SerialName("file_id") val fileId: String? = null,
    val filename: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    val url: String? = null
)

@Serializable
data class FileListResponse(
    val files: List<FileMetadata> = emptyList()
)

@Serializable
data class FileDeleteResponse(
    val deleted: Boolean? = null,
    @SerialName("file_id") val fileId: String? = null
)

/**
 * File Storage service module.
 * Provides file upload (via presigned URL), listing, metadata retrieval, and deletion.
 * Available on all platforms.
 */
class FileStorageService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "file-storage"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Generate a presigned URL for uploading a file.
     * The client should PUT the file bytes to the returned upload_url within expires_in seconds.
     *
     * @param filename Original filename (max 255 chars)
     * @param contentType MIME type of the file (must be in the server allowlist)
     */
    suspend fun getUploadUrl(filename: String, contentType: String): UploadUrlResponse {
        val body = """{"filename":"$filename","content_type":"$contentType"}"""
        return http.post("/apps/${http.appId}/files/upload-url", body, AuthMode.BEARER)
    }

    /** List all files uploaded by the authenticated user for the current app. */
    suspend fun listFiles(): FileListResponse =
        http.get("/apps/${http.appId}/files", authMode = AuthMode.BEARER)

    /**
     * Get metadata and a presigned download URL for a specific file.
     * Only the file owner can access the file.
     *
     * @param fileId The file ID to retrieve
     */
    suspend fun getFile(fileId: String): FileMetadata {
        val encoded = java.net.URLEncoder.encode(fileId, "UTF-8")
        return http.get("/apps/${http.appId}/files/$encoded", authMode = AuthMode.BEARER)
    }

    /**
     * Delete a file from storage.
     * Only the file owner can delete the file.
     *
     * @param fileId The file ID to delete
     */
    suspend fun deleteFile(fileId: String): FileDeleteResponse {
        val encoded = java.net.URLEncoder.encode(fileId, "UTF-8")
        return http.delete("/apps/${http.appId}/files/$encoded", authMode = AuthMode.BEARER)
    }
}
