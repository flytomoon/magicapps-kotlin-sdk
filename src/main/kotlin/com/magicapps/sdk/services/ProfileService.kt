package com.magicapps.sdk.services

import com.magicapps.sdk.UserProfile
import com.magicapps.sdk.UserProfilePublic
import com.magicapps.sdk.core.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Profile service module.
 * Provides access to user profiles — get/update the authenticated user's profile,
 * and view other users' public profiles.
 * Available on all platforms.
 *
 * Note: deleteProfile is intentionally not exposed — it is considered too destructive
 * for client SDK usage.
 */
class ProfileService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "profile"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** Get the authenticated user's profile. */
    suspend fun getProfile(): UserProfile =
        http.get("/apps/${http.appId}/profile", authMode = AuthMode.BEARER)

    /**
     * Update the authenticated user's profile.
     * Only the provided fields are updated; omitted fields are preserved.
     * Preferences and custom_fields are shallow-merged with existing values.
     *
     * @param displayName Display name (max 100 chars)
     * @param avatarUrl Avatar URL (max 2048 chars)
     * @param bio Bio text (max 500 chars)
     * @param preferences Arbitrary key-value preferences (shallow-merged)
     * @param customFields Arbitrary key-value custom fields (shallow-merged)
     */
    suspend fun updateProfile(
        displayName: String? = null,
        avatarUrl: String? = null,
        bio: String? = null,
        preferences: Map<String, JsonElement>? = null,
        customFields: Map<String, JsonElement>? = null
    ): UserProfile {
        val fields = mutableMapOf<String, JsonElement>()
        if (displayName != null) fields["display_name"] = kotlinx.serialization.json.JsonPrimitive(displayName)
        if (avatarUrl != null) fields["avatar_url"] = kotlinx.serialization.json.JsonPrimitive(avatarUrl)
        if (bio != null) fields["bio"] = kotlinx.serialization.json.JsonPrimitive(bio)
        if (preferences != null) fields["preferences"] = JsonObject(preferences)
        if (customFields != null) fields["custom_fields"] = JsonObject(customFields)
        val body = JsonObject(fields).toString()
        return http.put("/apps/${http.appId}/profile", body, AuthMode.BEARER)
    }

    /**
     * Get another user's public profile.
     * Returns only public fields: display_name, avatar_url, bio.
     *
     * @param userId The user ID to look up
     */
    suspend fun getPublicProfile(userId: String): UserProfilePublic {
        val encoded = java.net.URLEncoder.encode(userId, "UTF-8")
        return http.get("/apps/${http.appId}/profile/$encoded", authMode = AuthMode.BEARER)
    }
}
