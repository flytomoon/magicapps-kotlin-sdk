package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// --- Endpoint Types ---

@Serializable
data class CreateEndpointResponse(
    val slug: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("endpoint_path") val endpointPath: String,
    @SerialName("hmac_secret") val hmacSecret: String? = null,
    @SerialName("hmac_required") val hmacRequired: Boolean? = null
)

@Serializable
data class RevokeAndReplaceResponse(
    @SerialName("old_slug") val oldSlug: String,
    @SerialName("new_slug") val newSlug: String,
    @SerialName("new_endpoint_path") val newEndpointPath: String,
    @SerialName("revoked_expires_at") val revokedExpiresAt: Long,
    @SerialName("new_expires_at") val newExpiresAt: Long,
    @SerialName("hmac_secret") val hmacSecret: String? = null,
    @SerialName("hmac_required") val hmacRequired: Boolean? = null
)

@Serializable
data class RevokeEndpointResponse(
    val slug: String,
    val revoked: Boolean
)

@Serializable
data class PostEventResponse(
    val slug: String,
    val timestamp: Long,
    @SerialName("expires_at") val expiresAt: Long
)

@Serializable
data class ConsumedEvent(
    val slug: String,
    val timestamp: Long? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val text: String? = null,
    val keywords: List<String>? = null,
    @SerialName("raw_text") val rawText: String? = null,
    val metadata: JsonObject? = null,
    val empty: Boolean? = null
)

// --- HMAC Helpers ---

/** HMAC signature headers for authenticated event delivery. */
data class HmacSignatureHeaders(
    val signature: String,
    val timestamp: String
)

/**
 * Generate HMAC signature headers for posting a signed event.
 *
 * The signature is computed as: HMAC-SHA256(secret, "slug:timestamp:body")
 *
 * @param slug The endpoint slug
 * @param body The JSON body string being sent
 * @param secret The HMAC secret from the endpoint
 * @param timestampSec Optional Unix timestamp in seconds (defaults to now)
 * @return HmacSignatureHeaders with signature and timestamp
 */
fun generateHmacSignature(
    slug: String,
    body: String,
    secret: String,
    timestampSec: Long? = null
): HmacSignatureHeaders {
    val ts = timestampSec ?: (System.currentTimeMillis() / 1000)
    val message = "$slug:$ts:$body"
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    val signature = mac.doFinal(message.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return HmacSignatureHeaders(signature = signature, timestamp = ts.toString())
}

/**
 * Verify an HMAC signature on an incoming webhook payload.
 *
 * @param slug The endpoint slug
 * @param body The raw body string received
 * @param signature The X-Signature header value
 * @param timestamp The X-Timestamp header value
 * @param secret The HMAC secret for this endpoint
 * @param maxSkewSeconds Maximum allowed clock skew in seconds (default: 300)
 * @return true if the signature is valid and timestamp is within range
 */
fun verifyHmacSignature(
    slug: String,
    body: String,
    signature: String,
    timestamp: String,
    secret: String,
    maxSkewSeconds: Long = 300
): Boolean {
    val ts = timestamp.toLongOrNull() ?: return false
    val nowSec = System.currentTimeMillis() / 1000
    if (kotlin.math.abs(nowSec - ts) > maxSkewSeconds) return false

    val message = "$slug:$ts:$body"
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    val expected = mac.doFinal(message.toByteArray())
        .joinToString("") { "%02x".format(it) }

    // Constant-time comparison
    if (expected.length != signature.length) return false
    var mismatch = 0
    for (i in expected.indices) {
        mismatch = mismatch or (expected[i].code xor signature[i].code)
    }
    return mismatch == 0
}

// --- Endpoints Service ---

/**
 * Endpoints and Events service module.
 * Manages webhook endpoints and event consumption via the platform's
 * slug-based endpoint system.
 * Available on all platforms.
 */
class EndpointsService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "endpoints"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Create a new webhook endpoint.
     * Returns the slug, endpoint_path, and optionally an hmac_secret.
     */
    suspend fun create(): CreateEndpointResponse =
        http.post("/apps/${http.appId}/endpoints", "{}", AuthMode.OWNER)

    /**
     * Revoke an existing endpoint and create a replacement.
     * The old slug enters a grace period before full removal.
     */
    suspend fun revokeAndReplace(oldSlug: String): RevokeAndReplaceResponse {
        val body = """{"old_slug":"$oldSlug"}"""
        return http.post("/apps/${http.appId}/endpoints/revoke_and_replace", body, AuthMode.OWNER)
    }

    /**
     * Revoke an endpoint without creating a replacement.
     */
    suspend fun revoke(slug: String): RevokeEndpointResponse {
        val body = """{"slug":"$slug"}"""
        return http.post("/apps/${http.appId}/endpoints/revoke", body, AuthMode.OWNER)
    }

    /**
     * Post an event to an endpoint slug.
     * Optionally include HMAC signature headers for authenticated delivery.
     */
    suspend fun postEvent(slug: String, payload: String, hmacSecret: String? = null): PostEventResponse {
        val headers = if (hmacSecret != null) {
            val sig = generateHmacSignature(slug, payload, hmacSecret)
            mapOf("X-Signature" to sig.signature, "X-Timestamp" to sig.timestamp)
        } else null

        return http.post("/events/$slug", payload, AuthMode.NONE, extraHeaders = headers)
    }

    /**
     * Consume an event from an endpoint slug (single-slot, consume-on-read).
     * The event is deleted from the server after being read.
     */
    suspend fun consumeEvent(slug: String): ConsumedEvent =
        http.get("/events/$slug", authMode = AuthMode.NONE)
}
