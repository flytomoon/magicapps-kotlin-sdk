package com.magicapps.sdk.services

import com.magicapps.sdk.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- Device Types ---

@Serializable
data class DeviceEntry(
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("device_name") val deviceName: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    @SerialName("bluetooth_uuid") val bluetoothUuid: String? = null,
    val tags: List<String>? = null,
    val visibility: String? = null,
    val source: String? = null,
    val category: String? = null,
    val specs: JsonObject? = null
)

@Serializable
data class DeviceCatalogResponse(
    val devices: List<DeviceEntry>? = null,
    val items: List<DeviceEntry>? = null,
    val count: Int? = null
) {
    /** Convenience accessor returning devices from whichever field the API uses. */
    val allDevices: List<DeviceEntry> get() = devices ?: items ?: emptyList()
}

/**
 * Devices service module.
 * Provides read-only access to the merged device catalog
 * including hardcoded, catalog, and user-submitted devices.
 * Available on all platforms.
 */
class DevicesService(private val http: SdkHttpClient) : ServiceModule {
    override val name = "devices"
    override val platforms = emptyList<SdkPlatform>() // all platforms

    /** Fetch the device catalog for the current app. */
    suspend fun list(): DeviceCatalogResponse =
        http.get("/apps/${http.appId}/devices", authMode = AuthMode.NONE)

    /** Convenience: get a flat list of all devices. */
    suspend fun getAll(): List<DeviceEntry> = list().allDevices
}
