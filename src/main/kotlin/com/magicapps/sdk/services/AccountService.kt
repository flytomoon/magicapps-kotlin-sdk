package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Account Types ---

@Serializable
data class AccountDeleteResponse(
    val status: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

/**
 * Account service module.
 * Provides account-level operations such as account deletion (GDPR right to erasure).
 * Available on all platforms.
 *
 * Note: exportAccountData and consent management are intentionally not exposed —
 * they are only available in the TypeScript SDK.
 */
class AccountService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "account"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Delete the authenticated user's account (GDPR right to erasure).
     * Soft-deletes the account by scrubbing PII but retaining the account shell
     * and financial records for legal compliance. Deletes user files from storage.
     *
     * @param reason Optional reason for account deletion (max 2000 chars)
     */
    suspend fun deleteAccount(reason: String? = null): AccountDeleteResponse {
        val body = if (reason != null) """{"reason":"$reason"}""" else null
        return http.request("DELETE", "/apps/${http.appId}/account", body, null, AuthMode.BEARER, null)
    }
}
