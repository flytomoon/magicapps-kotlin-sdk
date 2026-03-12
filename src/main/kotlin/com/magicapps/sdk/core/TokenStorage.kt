package com.magicapps.sdk.core

/**
 * Interface for pluggable token storage backends.
 *
 * Implement this to provide custom secure storage for tokens.
 * The SDK ships with [EncryptedFileTokenStorage] (default) and [InMemoryTokenStorage].
 *
 * Android developers can implement this with EncryptedSharedPreferences:
 * ```kotlin
 * class SharedPrefsTokenStorage(context: Context) : TokenStorage {
 *     private val prefs = EncryptedSharedPreferences.create(...)
 *     override fun save(key: String, value: String) { prefs.edit().putString(key, value).apply() }
 *     override fun load(key: String): String? = prefs.getString(key, null)
 *     override fun delete(key: String) { prefs.edit().remove(key).apply() }
 *     override fun deleteAll() { prefs.edit().clear().apply() }
 * }
 * ```
 */
interface TokenStorage {
    /** Save a token value for the given key. */
    fun save(key: String, value: String)
    /** Load a token value for the given key. Returns null if not found. */
    fun load(key: String): String?
    /** Delete a token value for the given key. */
    fun delete(key: String)
    /** Delete all tokens managed by this storage instance. */
    fun deleteAll()
}

/** Well-known keys used by the SDK for token storage. */
object TokenStorageKeys {
    const val ACCESS_TOKEN = "com.magicapps.sdk.accessToken"
    const val REFRESH_TOKEN = "com.magicapps.sdk.refreshToken"
    const val OWNER_TOKEN = "com.magicapps.sdk.ownerToken"
}
