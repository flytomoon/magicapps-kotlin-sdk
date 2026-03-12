package com.magicapps.sdk.core

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Stores tokens encrypted at rest using AES-256-GCM.
 *
 * Tokens are stored in individual files within a directory, encrypted with a
 * randomly generated AES-256 key. The key is stored alongside the token files.
 * This provides encryption-at-rest protection so tokens are not stored in plaintext.
 *
 * For stronger protection on Android, implement [TokenStorage] with
 * `EncryptedSharedPreferences` which backs its master key with the Android Keystore.
 *
 * ```kotlin
 * // Default directory: ~/.magicapps/tokens/
 * val storage = EncryptedFileTokenStorage()
 *
 * // Custom directory
 * val storage = EncryptedFileTokenStorage(File("/secure/path/tokens"))
 * ```
 *
 * @param directory The directory to store encrypted token files in.
 *   Defaults to `~/.magicapps/tokens/`.
 */
class EncryptedFileTokenStorage(
    private val directory: File = File(System.getProperty("user.home"), ".magicapps/tokens")
) : TokenStorage {

    private val keyFile = File(directory, ".keyfile")
    private val gcmTagLength = 128
    private val ivLength = 12

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_SIZE = 256
    }

    init {
        directory.mkdirs()
    }

    @Synchronized
    override fun save(key: String, value: String) {
        try {
            val secretKey = getOrCreateKey()
            val iv = ByteArray(ivLength).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(gcmTagLength, iv))
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            // Store as: IV (12 bytes) + encrypted data
            val combined = iv + encrypted
            tokenFile(key).writeBytes(combined)
        } catch (e: Exception) {
            throw TokenStorageException("Failed to save token: ${e.message}", e)
        }
    }

    @Synchronized
    override fun load(key: String): String? {
        val file = tokenFile(key)
        if (!file.exists()) return null

        return try {
            val secretKey = getOrCreateKey()
            val combined = file.readBytes()
            if (combined.size < ivLength) return null

            val iv = combined.copyOfRange(0, ivLength)
            val encrypted = combined.copyOfRange(ivLength, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(gcmTagLength, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails (corrupted file, key mismatch), remove the file
            file.delete()
            null
        }
    }

    @Synchronized
    override fun delete(key: String) {
        tokenFile(key).delete()
    }

    @Synchronized
    override fun deleteAll() {
        tokenFile(TokenStorageKeys.ACCESS_TOKEN).delete()
        tokenFile(TokenStorageKeys.REFRESH_TOKEN).delete()
        tokenFile(TokenStorageKeys.OWNER_TOKEN).delete()
    }

    private fun tokenFile(key: String): File {
        // Use a safe filename derived from the key
        val safeName = key.replace(".", "_")
        return File(directory, safeName)
    }

    private fun getOrCreateKey(): SecretKey {
        return if (keyFile.exists()) {
            val keyBytes = keyFile.readBytes()
            SecretKeySpec(keyBytes, KEY_ALGORITHM)
        } else {
            val generator = KeyGenerator.getInstance(KEY_ALGORITHM)
            generator.init(KEY_SIZE, SecureRandom())
            val key = generator.generateKey()
            keyFile.writeBytes(key.encoded)
            key
        }
    }
}

/** Exception thrown when token storage operations fail. */
class TokenStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
