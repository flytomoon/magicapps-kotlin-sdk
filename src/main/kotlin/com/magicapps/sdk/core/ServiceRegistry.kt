package com.magicapps.sdk.core

/**
 * Platform identifiers for conditional module availability.
 */
enum class SdkPlatform {
    IOS,
    ANDROID,
    WEB
}

/**
 * Interface that all service modules implement.
 */
interface ServiceModule {
    /** The name of this service module. */
    val name: String
    /** Platforms this module is available on (empty = all platforms). */
    val platforms: List<SdkPlatform>
}

/**
 * Registry that manages service modules and enforces platform-conditional availability.
 */
class ServiceRegistry(private val platform: SdkPlatform) {
    private val modules = mutableMapOf<String, ServiceModule>()

    /** Register a service module. */
    fun register(module: ServiceModule) {
        modules[module.name] = module
    }

    /** Get a registered service module by name. Throws PlatformException if unavailable. */
    @Suppress("UNCHECKED_CAST")
    fun <T : ServiceModule> get(name: String): T? {
        val module = modules[name] ?: return null
        if (module.platforms.isNotEmpty() && platform !in module.platforms) {
            throw PlatformException(
                moduleName = name,
                currentPlatform = platform.name.lowercase(),
                supportedPlatforms = module.platforms.map { it.name.lowercase() }
            )
        }
        return module as? T
    }

    /** Check if a module is available on the current platform. */
    fun has(name: String): Boolean {
        val module = modules[name] ?: return false
        return module.platforms.isEmpty() || platform in module.platforms
    }

    /** List all modules available on the current platform. */
    fun listAvailable(): List<ServiceModule> =
        modules.values.filter { it.platforms.isEmpty() || platform in it.platforms }

    /** Get the current platform. */
    fun getPlatform(): SdkPlatform = platform
}
