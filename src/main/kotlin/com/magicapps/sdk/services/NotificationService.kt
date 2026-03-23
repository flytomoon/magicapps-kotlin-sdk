package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Notification Types ---

@Serializable
data class DeviceRegisterResponse(
    val registered: Boolean? = null,
    @SerialName("device_id") val deviceId: String? = null
)

@Serializable
data class DeviceUnregisterResponse(
    val unregistered: Boolean? = null,
    @SerialName("device_id") val deviceId: String? = null
)

/**
 * Notification service module.
 * Provides device registration and unregistration for push notifications.
 * Available on all platforms.
 *
 * Note: getNotifications is intentionally not exposed — it is not useful client-side.
 */
class NotificationService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "notifications"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /**
     * Register a device for push notifications.
     *
     * @param token Device push token (APNs/FCM token or Web Push subscription JSON)
     * @param platform Platform identifier: "apns", "fcm", or "web"
     * @param deviceId Optional stable device identifier for upsert
     */
    suspend fun registerDevice(
        token: String,
        platform: String,
        deviceId: String? = null
    ): DeviceRegisterResponse {
        val deviceIdField = if (deviceId != null) ""","device_id":"$deviceId"""" else ""
        val body = """{"token":"$token","platform":"$platform"$deviceIdField}"""
        return http.post("/apps/${http.appId}/notifications/register", body, AuthMode.BEARER)
    }

    /**
     * Unregister a device from push notifications.
     *
     * @param deviceId The device ID to unregister
     */
    suspend fun unregisterDevice(deviceId: String): DeviceUnregisterResponse {
        val encoded = java.net.URLEncoder.encode(deviceId, "UTF-8")
        return http.delete("/apps/${http.appId}/notifications/register/$encoded", authMode = AuthMode.BEARER)
    }
}
