package com.magicapps.sdk.core

import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.util.Base64

/**
 * Configuration for certificate pinning.
 *
 * Certificate pinning protects against man-in-the-middle attacks by validating
 * that the server's TLS certificate matches a known set of public key hashes.
 *
 * Pins are SHA-256 hashes of the Subject Public Key Info (SPKI), encoded as
 * base64 strings prefixed with "sha256/" (the standard format used by HTTP
 * Public Key Pinning and OkHttp).
 *
 * ```kotlin
 * val pinning = CertificatePinningConfig(
 *     pins = listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
 *     includeBuiltInPins = true
 * )
 * val config = SdkConfig(
 *     baseUrl = "https://api.magicapps.dev",
 *     appId = "my-app",
 *     certificatePinning = pinning
 * )
 * ```
 */
data class CertificatePinningConfig(
    /** SHA-256 SPKI pin hashes (base64 encoded, "sha256/" prefixed). */
    val pins: List<String> = emptyList(),
    /** Whether certificate pinning is enabled. Set to `false` for development/testing. */
    val enabled: Boolean = true,
    /** Whether to include built-in pins for the Magic Apps Cloud API domain. */
    val includeBuiltInPins: Boolean = true
) {
    /** All effective pins (custom + built-in if enabled). */
    internal val effectivePins: List<String>
        get() {
            val allPins = pins.toMutableList()
            if (includeBuiltInPins) {
                allPins.addAll(BUILT_IN_PINS)
            }
            return allPins
        }

    companion object {
        /**
         * Built-in pins for the Magic Apps Cloud API domain (api.magicapps.dev).
         * These correspond to the current and backup certificate public key hashes.
         * Update these when rotating certificates.
         */
        internal val BUILT_IN_PINS = listOf(
            // Primary: Let's Encrypt ISRG Root X1 (intermediate CA)
            "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",
            // Backup: Let's Encrypt E5 intermediate
            "sha256/J2/oqMTsdhFWW/n85tys6b4yDBtb6idZayIEBx7QTxA="
        )
    }
}

/**
 * Certificate pinning exception.
 *
 * Thrown when the server's certificate does not match any pinned public key hash.
 * This indicates a potential man-in-the-middle attack or a certificate rotation
 * that requires updating the SDK's pin configuration. There is no silent fallback.
 */
class CertificatePinningException(
    val host: String
) : SdkException(
    "Certificate pinning failure: The server certificate for $host did not match " +
            "any pinned public key. This may indicate a man-in-the-middle attack or a " +
            "certificate rotation. Update your CertificatePinningConfig pins or contact support."
)

/**
 * X509TrustManager that validates server certificates against pinned public key hashes.
 *
 * This trust manager first delegates to the system default trust manager to ensure
 * the certificate chain is valid, then additionally checks that at least one
 * certificate in the chain has a public key hash matching the configured pins.
 */
internal class PinningTrustManager(
    private val pins: List<ByteArray>,
    private val defaultTrustManager: X509TrustManager
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // First, validate with the system trust manager
        defaultTrustManager.checkServerTrusted(chain, authType)

        // Then verify at least one certificate in the chain matches a pin
        if (chain == null || chain.isEmpty()) {
            throw CertificatePinningException("unknown")
        }

        val matched = chain.any { cert ->
            val publicKeyHash = hashPublicKey(cert)
            pins.any { pin -> pin.contentEquals(publicKeyHash) }
        }

        if (!matched) {
            val host = try {
                chain[0].subjectX500Principal.name
            } catch (_: Exception) {
                "unknown"
            }
            throw CertificatePinningException(host)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        defaultTrustManager.acceptedIssuers

    /** Compute SHA-256 hash of a certificate's public key. */
    private fun hashPublicKey(certificate: X509Certificate): ByteArray {
        val publicKeyEncoded = certificate.publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(publicKeyEncoded)
    }
}

/**
 * Creates an SSLContext configured with certificate pinning.
 */
internal object CertificatePinnerFactory {
    fun createPinnedSSLContext(config: CertificatePinningConfig): SSLContext {
        val pinHashes = config.effectivePins.map { pin ->
            val base64 = if (pin.startsWith("sha256/")) pin.removePrefix("sha256/") else pin
            Base64.getDecoder().decode(base64)
        }

        // Get the default trust manager
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as java.security.KeyStore?)
        val defaultTrustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

        val pinningTrustManager = PinningTrustManager(pinHashes, defaultTrustManager)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(pinningTrustManager), null)
        return sslContext
    }

    /**
     * Apply certificate pinning to an HttpsURLConnection.
     */
    fun applyPinning(connection: HttpsURLConnection, sslContext: SSLContext) {
        connection.sslSocketFactory = sslContext.socketFactory
    }
}
