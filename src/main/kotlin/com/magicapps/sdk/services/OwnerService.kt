package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Owner Types ---

@Serializable
data class OwnerRegisterResponse(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("app_id") val appId: String,
    val token: String? = null,
    val status: String? = null
)

@Serializable
data class OwnerMigrateResponse(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("app_id") val appId: String,
    @SerialName("user_id") val userId: String? = null,
    val status: String? = null
)

/**
 * Owner service module.
 * Manages device-owner registration and migration to full user accounts.
 * Available on all platforms.
 */
class OwnerService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "owner"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Register a new device owner.
     *
     * @param deviceOwnerId A unique identifier for the device owner
     * @param appId The app to register the owner under
     * @param hcaptchaToken Optional hCaptcha token for bot protection
     */
    suspend fun registerOwner(
        deviceOwnerId: String,
        appId: String,
        hcaptchaToken: String? = null
    ): OwnerRegisterResponse {
        val captchaField = if (hcaptchaToken != null) ""","hcaptcha_token":"$hcaptchaToken"""" else ""
        val body = """{"device_owner_id":"$deviceOwnerId","app_id":"$appId"$captchaField}"""
        return http.post("/owner/register", body, AuthMode.NONE)
    }

    /**
     * Migrate a device owner to a full user account.
     *
     * @param deviceOwnerId The device owner ID to migrate
     * @param appId The app the owner belongs to
     */
    suspend fun migrateOwnerToUser(
        deviceOwnerId: String,
        appId: String
    ): OwnerMigrateResponse {
        val body = """{"device_owner_id":"$deviceOwnerId","app_id":"$appId"}"""
        return http.post("/owner/migrate", body, AuthMode.NONE)
    }
}
