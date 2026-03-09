package com.magicapps.sdk.core

/**
 * Metadata describing a deprecated SDK method.
 *
 * Used to track deprecation history for documentation and migration guides.
 * The Kotlin SDK uses the built-in `@Deprecated` annotation with `ReplaceWith`
 * for compile-time warnings. This class provides runtime metadata for tooling.
 *
 * Example:
 * ```kotlin
 * // Kotlin deprecation with ReplaceWith suggestion:
 * @Deprecated(
 *     message = "Use getAppInfo() instead. Deprecated since v0.2.0.",
 *     replaceWith = ReplaceWith("getAppInfo()"),
 *     level = DeprecationLevel.WARNING
 * )
 * suspend fun fetchApp(): AppInfo = getAppInfo()
 * ```
 *
 * @property methodName The name of the deprecated method.
 * @property since The SDK version in which this method was deprecated.
 * @property message Migration hint explaining what to use instead.
 * @property removeIn Optional version in which this method will be removed.
 */
data class DeprecationInfo(
    val methodName: String,
    val since: String,
    val message: String,
    val removeIn: String? = null
)

/**
 * Registry of deprecated methods in the Kotlin SDK.
 *
 * Provides a central catalog of all deprecated methods for tooling,
 * documentation generation, and migration guide automation.
 *
 * The Kotlin SDK uses `@Deprecated(replaceWith = ReplaceWith(...))` for
 * compile-time deprecation warnings with IDE quick-fix support. This registry
 * complements that with version info and removal timeline metadata.
 */
object DeprecationRegistry {
    /** All deprecated methods in the current SDK version. */
    val entries: List<DeprecationInfo> = listOf(
        // Example entry (uncomment when actual deprecations exist):
        // DeprecationInfo(
        //     methodName = "fetchApp()",
        //     since = "0.2.0",
        //     message = "Use getAppInfo() instead",
        //     removeIn = "1.0.0"
        // ),
    )

    /**
     * Get deprecation info for a specific method.
     * @param methodName The method name to look up.
     * @return The deprecation info, or null if the method is not deprecated.
     */
    fun info(methodName: String): DeprecationInfo? =
        entries.firstOrNull { it.methodName == methodName }

    /**
     * All methods scheduled for removal in a given version.
     * @param version The version to check.
     * @return List of deprecation entries scheduled for removal in that version.
     */
    fun scheduledForRemoval(version: String): List<DeprecationInfo> =
        entries.filter { it.removeIn == version }
}
