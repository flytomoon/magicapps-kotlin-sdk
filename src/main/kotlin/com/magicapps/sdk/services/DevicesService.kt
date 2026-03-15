package com.magicapps.sdk.services

import com.magicapps.sdk.Device
import com.magicapps.sdk.core.*
import kotlinx.serialization.Serializable

// --- Device Types ---

@Serializable
data class DeviceCatalogResponse(
    val devices: List<Device>? = null,
    val items: List<Device>? = null,
    val count: Int? = null
) {
    /** Convenience accessor returning devices from whichever field the API uses. */
    val allDevices: List<Device> get() = devices ?: items ?: emptyList()
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
    suspend fun getAll(): List<Device> = list().allDevices
}
