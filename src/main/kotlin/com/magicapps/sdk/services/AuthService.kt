package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.Serializable

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

/**
 * Authentication service module. Available on all platforms.
 */
class AuthService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "auth"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** Authenticate with email and password. */
    suspend fun login(email: String, password: String): LoginResponse {
        val body = """{"email":"$email","password":"$password"}"""
        val response: LoginResponse = http.post("/auth/login", body, AuthMode.NONE)
        http.tokenManager.setTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken
        )
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
}
