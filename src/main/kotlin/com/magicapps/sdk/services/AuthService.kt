package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.Serializable

// --- Common Auth Types ---

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresIn: Int? = null
)

@Serializable
data class RegisterResponse(
    val userId: String,
    val email: String,
    val confirmed: Boolean
)

@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Int? = null
)

@Serializable
data class LinkProviderResponse(
    val success: Boolean,
    val linkedProviders: List<String>
)

// --- Passkey Types ---

@Serializable
data class PasskeyRegisterOptionsResponse(
    val challenge: String,
    val rp: RelyingParty,
    val user: PasskeyUser,
    val timeout: Int? = null
) {
    @Serializable
    data class RelyingParty(val id: String, val name: String)
    @Serializable
    data class PasskeyUser(val id: String, val name: String, val displayName: String)
}

@Serializable
data class PasskeyRegisterVerifyResponse(
    val success: Boolean,
    val credentialId: String
)

@Serializable
data class PasskeyAuthOptionsResponse(
    val challenge: String,
    val timeout: Int? = null,
    val rpId: String? = null,
    val userVerification: String? = null
)

@Serializable
data class PasskeyAuthVerifyResponse(
    val accessToken: String,
    val refreshToken: String? = null
)

// --- Email Magic Link Types ---

@Serializable
data class EmailMagicLinkResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class EmailMagicLinkVerifyResponse(
    val accessToken: String,
    val refreshToken: String? = null
)

// --- Google Sign-In Types ---

@Serializable
data class GoogleExchangeResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val isNewUser: Boolean? = null
)

/**
 * Core authentication service module.
 * Provides email/password auth, passkeys, email magic links,
 * token refresh, and identity linking.
 * Available on all platforms.
 */
class AuthService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "auth"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** Authenticate with email and password. */
    suspend fun login(email: String, password: String): LoginResponse {
        val body = """{"email":"$email","password":"$password"}"""
        val response: LoginResponse = http.post("/auth/login", body, AuthMode.NONE)
        http.tokenManager.setTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
        return response
    }

    /** Register a new user account. */
    suspend fun register(email: String, password: String, name: String? = null): RegisterResponse {
        val nameField = if (name != null) ""","name":"$name"""" else ""
        val body = """{"email":"$email","password":"$password"$nameField}"""
        return http.post("/auth/register", body, AuthMode.NONE)
    }

    /** Log out and clear stored tokens. */
    suspend fun logout() {
        try {
            http.post<Unit>("/auth/logout", authMode = AuthMode.BEARER)
        } finally {
            http.tokenManager.clearTokens()
        }
    }

    // --- Token Refresh ---

    /** Refresh access token using stored refresh token. */
    suspend fun refreshToken(refreshToken: String): TokenRefreshResponse {
        val body = """{"refresh_token":"$refreshToken"}"""
        val response: TokenRefreshResponse = http.post("/auth/client/refresh", body, AuthMode.NONE)
        http.tokenManager.setTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
        return response
    }

    // --- Identity Linking ---

    /** Link an additional auth provider to the current account. */
    suspend fun linkProvider(provider: String, token: String): LinkProviderResponse {
        val body = """{"provider":"$provider","token":"$token"}"""
        return http.post("/auth/client/link", body, AuthMode.BEARER)
    }

    // --- Passkey Registration ---

    /** Get passkey registration options (challenge) from the server. */
    suspend fun getPasskeyRegisterOptions(): PasskeyRegisterOptionsResponse {
        return http.post("/auth/client/passkey/register/options", "{}", AuthMode.BEARER)
    }

    /** Complete passkey registration by verifying the credential. */
    suspend fun verifyPasskeyRegistration(credentialJson: String): PasskeyRegisterVerifyResponse {
        return http.post("/auth/client/passkey/register/verify", credentialJson, AuthMode.BEARER)
    }

    // --- Passkey Authentication ---

    /** Get passkey authentication options (challenge) from the server. */
    suspend fun getPasskeyAuthOptions(): PasskeyAuthOptionsResponse {
        return http.post("/auth/client/passkey/authenticate/options", "{}", AuthMode.NONE)
    }

    /** Complete passkey authentication by verifying the assertion. */
    suspend fun verifyPasskeyAuth(assertionJson: String): PasskeyAuthVerifyResponse {
        val response: PasskeyAuthVerifyResponse = http.post("/auth/client/passkey/authenticate/verify", assertionJson, AuthMode.NONE)
        http.tokenManager.setTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
        return response
    }

    // --- Email Magic Link ---

    /** Request an email magic link for passwordless sign-in. */
    suspend fun requestEmailMagicLink(email: String): EmailMagicLinkResponse {
        val body = """{"email":"$email"}"""
        return http.post("/auth/client/email/request", body, AuthMode.NONE)
    }

    /** Verify an email magic link token to complete sign-in. */
    suspend fun verifyEmailMagicLink(token: String): EmailMagicLinkVerifyResponse {
        val body = """{"token":"$token"}"""
        val response: EmailMagicLinkVerifyResponse = http.post("/auth/client/email/verify", body, AuthMode.NONE)
        http.tokenManager.setTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
        return response
    }
}

/**
 * Google Sign-In authentication module.
 * Only available on Android.
 */
class GoogleAuthService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "google-auth"
    override val platforms = listOf(SdkPlatform.ANDROID)

    /** Exchange a Google ID token for MagicApps access + refresh tokens. */
    suspend fun exchangeToken(idToken: String, accessToken: String? = null): GoogleExchangeResponse {
        val accessTokenField = if (accessToken != null) ""","accessToken":"$accessToken"""" else ""
        val body = """{"idToken":"$idToken"$accessTokenField}"""
        val response: GoogleExchangeResponse = http.post("/auth/client/google/exchange", body, AuthMode.NONE)
        http.tokenManager.setTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
        return response
    }
}
