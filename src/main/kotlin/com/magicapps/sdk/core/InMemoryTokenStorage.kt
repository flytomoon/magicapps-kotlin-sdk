package com.magicapps.sdk.core

/**
 * Stores tokens in memory only. Tokens are lost when the process exits.
 *
 * Use this when you don't want tokens persisted to disk, or in environments
 * where file-system access is restricted (e.g., unit tests).
 *
 * ```kotlin
 * val config = SdkConfig(
 *     baseUrl = "https://api.magicapps.dev",
 *     appId = "my-app",
 *     tokenStorage = InMemoryTokenStorage()
 * )
 * ```
 */
class InMemoryTokenStorage : TokenStorage {
    private val store = mutableMapOf<String, String>()

    @Synchronized
    override fun save(key: String, value: String) {
        store[key] = value
    }

    @Synchronized
    override fun load(key: String): String? = store[key]

    @Synchronized
    override fun delete(key: String) {
        store.remove(key)
    }

    @Synchronized
    override fun deleteAll() {
        store.clear()
    }
}
