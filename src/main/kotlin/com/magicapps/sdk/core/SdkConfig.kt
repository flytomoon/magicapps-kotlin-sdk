package com.magicapps.sdk.core

/**
 * Configuration for initializing the Magic Apps Cloud SDK.
 */
data class SdkConfig(
    /** The base URL of the MagicApps API. */
    val baseUrl: String,
    /** The app_id that scopes all API requests to a specific tenant. */
    val appId: String,
    /** Optional Bearer JWT token for user authentication. */
    var accessToken: String? = null,
    /** Optional refresh token for automatic token renewal. */
    var refreshToken: String? = null,
    /** Optional owner token for owner-level authentication. */
    var ownerToken: String? = null,
    /** Number of retries for failed requests (default: 2). */
    val retries: Int = 2,
    /** Base delay between retries in ms (default: 250). */
    val retryDelayMs: Long = 250,
    /** Callback invoked when tokens are refreshed. */
    var onTokenRefresh: ((TokenPair) -> Unit)? = null,
    /** Callback invoked when token refresh fails. */
    var onAuthError: ((SdkException) -> Unit)? = null,
    /**
     * Certificate pinning configuration. When set, the SDK validates that the
     * server's TLS certificate matches one of the configured public key hashes.
     * Set [CertificatePinningConfig.enabled] to `false` for development/testing.
     */
    val certificatePinning: CertificatePinningConfig? = null,
    /**
     * Token storage backend. Defaults to [EncryptedFileTokenStorage] for
     * encrypted persistence. Pass [InMemoryTokenStorage] to opt out, or
     * provide a custom [TokenStorage] implementation (e.g., EncryptedSharedPreferences
     * on Android).
     */
    val tokenStorage: TokenStorage = EncryptedFileTokenStorage()
)

/**
 * A pair of access + refresh tokens.
 */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String? = null
)

/**
 * Auth mode for a request.
 */
enum class AuthMode {
    BEARER,
    OWNER,
    NONE
}
