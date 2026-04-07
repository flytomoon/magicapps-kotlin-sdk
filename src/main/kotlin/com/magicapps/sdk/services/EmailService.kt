package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- Email Image/Text Types ---

@Serializable
data class CreateImageTokenResponse(
    val token: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("expires_at") val expiresAt: Long
)

@Serializable
data class CreateTextTokenResponse(
    val token: String,
    @SerialName("text_url") val textUrl: String,
    @SerialName("expires_at") val expiresAt: Long
)

@Serializable
data class EmailTokenStatus(
    val token: String,
    val type: String,
    val state: String,
    @SerialName("ready_at") val readyAt: Long? = null,
    @SerialName("consumed_at") val consumedAt: Long? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null
)

/**
 * Internal empty response type for 204 No Content responses.
 * Used by upload methods that return no body.
 */
@Serializable
internal class EmptyResponse

// --- Email Service ---

/**
 * Email image and text hosting service module.
 * Provides token-based image and text hosting for email content.
 * Available on all platforms.
 *
 * All methods require owner-level authentication ([AuthMode.OWNER]).
 */
class EmailService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "email"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Create a token for hosting an email image.
     * The returned [CreateImageTokenResponse.imageUrl] can be embedded in email HTML
     * before the image is uploaded.
     *
     * @param ttlSeconds Optional time-to-live in seconds for the token
     * @param metadata Optional metadata to associate with the token
     */
    suspend fun createImageToken(
        ttlSeconds: Int? = null,
        metadata: JsonObject? = null
    ): CreateImageTokenResponse {
        val fields = mutableListOf<String>()
        if (ttlSeconds != null) fields.add(""""ttl_seconds":$ttlSeconds""")
        if (metadata != null) fields.add(""""metadata":$metadata""")
        val body = "{${fields.joinToString(",")}}"
        return http.post("/apps/${http.appId}/routines/email-image/tokens", body, AuthMode.OWNER)
    }

    /**
     * Upload a JPEG image to a previously created image token.
     * The server returns 204 No Content on success.
     *
     * **Android callers:** Convert your `Bitmap` to a JPEG `ByteArray` before calling this method.
     * For example:
     * ```kotlin
     * val stream = ByteArrayOutputStream()
     * bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
     * val jpegBytes = stream.toByteArray()
     * emailService.uploadImage(token, jpegBytes)
     * ```
     *
     * @param token The image token from [createImageToken]
     * @param imageData JPEG image data as a ByteArray (NOT a Bitmap)
     * @param transform Optional transform type (e.g., "image", "screenshot")
     * @param query Required when [transform] is not "image" — the query string for the transform
     * @param metadata Optional metadata to associate with the upload
     */
    suspend fun uploadImage(
        token: String,
        imageData: ByteArray,
        transform: String? = null,
        query: String? = null,
        metadata: JsonObject? = null
    ) {
        val base64 = java.util.Base64.getEncoder().encodeToString(imageData)
        val fields = mutableListOf(""""image_jpeg_base64":"$base64"""")
        if (transform != null) fields.add(""""transform":"$transform"""")
        if (query != null) fields.add(""""query":"$query"""")
        if (metadata != null) fields.add(""""metadata":$metadata""")
        val body = "{${fields.joinToString(",")}}"
        http.post<EmptyResponse>("/apps/${http.appId}/routines/email-image/$token", body, AuthMode.OWNER)
    }

    /**
     * Create a token for hosting email text content.
     * The returned [CreateTextTokenResponse.textUrl] can be embedded in email HTML
     * before the text is uploaded.
     *
     * @param ttlSeconds Optional time-to-live in seconds for the token
     * @param metadata Optional metadata to associate with the token
     */
    suspend fun createTextToken(
        ttlSeconds: Int? = null,
        metadata: JsonObject? = null
    ): CreateTextTokenResponse {
        val fields = mutableListOf<String>()
        if (ttlSeconds != null) fields.add(""""ttl_seconds":$ttlSeconds""")
        if (metadata != null) fields.add(""""metadata":$metadata""")
        val body = "{${fields.joinToString(",")}}"
        return http.post("/apps/${http.appId}/routines/email-text/tokens", body, AuthMode.OWNER)
    }

    /**
     * Upload text content to a previously created text token.
     * The server returns 204 No Content on success.
     *
     * @param token The text token from [createTextToken]
     * @param sentence The text content to upload (this is the primary content field)
     * @param metadata Optional metadata to associate with the upload
     */
    suspend fun uploadText(
        token: String,
        sentence: String,
        metadata: JsonObject? = null
    ) {
        val escapedSentence = sentence.replace("\\", "\\\\").replace("\"", "\\\"")
        val fields = mutableListOf(""""sentence":"$escapedSentence"""")
        if (metadata != null) fields.add(""""metadata":$metadata""")
        val body = "{${fields.joinToString(",")}}"
        http.post<EmptyResponse>("/apps/${http.appId}/routines/email-text/$token", body, AuthMode.OWNER)
    }

    /**
     * Get the current status of an email token (image or text).
     *
     * @param token The token to check
     * @return [EmailTokenStatus] with the token's current state
     */
    suspend fun getTokenStatus(token: String): EmailTokenStatus =
        http.get("/apps/${http.appId}/routines/email-status/$token", authMode = AuthMode.OWNER)
}
