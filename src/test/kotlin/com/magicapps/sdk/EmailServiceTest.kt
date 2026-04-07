package com.magicapps.sdk

import com.magicapps.sdk.core.*
import com.magicapps.sdk.services.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Contract validation tests for the EmailService.
 *
 * Verifies that every EmailService method:
 *   1) Targets the correct URL path
 *   2) Uses the correct HTTP method
 *   3) Sends the correct request body fields
 *   4) Uses AuthMode.OWNER (owner token in Authorization header)
 *   5) Response data classes deserialize correctly
 *
 * Uses OkHttp MockWebServer to intercept HttpURLConnection requests.
 * Each test gets its own server instance to avoid request queue interference.
 */
class EmailServiceTest {

    companion object {
        private const val TEST_APP_ID = "test-app"
        private const val OWNER_TOKEN = "test-owner-token"

        // --- Email fixtures ---

        // Source: lambda/email_images/index.js — POST /apps/{app_id}/routines/email-image/tokens
        const val FIXTURE_IMAGE_TOKEN = """{"token":"img-tok-1","image_url":"https://cdn.example.com/img/img-tok-1","expires_at":1749676000}"""

        // Source: lambda/email_images/index.js — POST /apps/{app_id}/routines/email-text/tokens
        const val FIXTURE_TEXT_TOKEN = """{"token":"txt-tok-1","text_url":"https://cdn.example.com/txt/txt-tok-1","expires_at":1749676000}"""

        // Source: lambda/email_images/index.js — GET /apps/{app_id}/routines/email-status/{token}
        const val FIXTURE_TOKEN_STATUS = """{"token":"img-tok-1","type":"image","state":"ready","ready_at":1741900000,"consumed_at":null,"expires_at":1749676000,"updated_at":1741900100}"""
    }

    private lateinit var server: MockWebServer
    private lateinit var baseUrl: String

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        baseUrl = server.url("/").toString().trimEnd('/')
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Create a fresh SdkHttpClient pointing at MockWebServer. */
    private fun createHttpClient(): SdkHttpClient {
        val config = SdkConfig(
            baseUrl = baseUrl,
            appId = TEST_APP_ID,
            ownerToken = OWNER_TOKEN,
            retries = 0,
            tokenStorage = InMemoryTokenStorage()
        )
        return SdkHttpClient(config)
    }

    /** Enqueue a JSON 200 response. */
    private fun enqueue(body: String, statusCode: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    /** Enqueue a 204 No Content response. */
    private fun enqueue204() {
        server.enqueue(MockResponse().setResponseCode(204))
    }

    // ========================================================================
    // createImageToken
    // ========================================================================

    @Test
    fun `createImageToken targets POST apps-appId-routines-email-image-tokens`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_IMAGE_TOKEN)

        val result = service.createImageToken()
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/apps/$TEST_APP_ID/routines/email-image/tokens", request.path)
        assertEquals("img-tok-1", result.token)
        assertEquals("https://cdn.example.com/img/img-tok-1", result.imageUrl)
        assertEquals(1749676000L, result.expiresAt)
    }

    @Test
    fun `createImageToken sends ttl_seconds and metadata when provided`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_IMAGE_TOKEN)

        val metadata = buildJsonObject { put("campaign", "welcome") }
        service.createImageToken(ttlSeconds = 3600, metadata = metadata)
        val request = server.takeRequest()

        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains(""""ttl_seconds":3600"""), "Body should contain ttl_seconds")
        assertTrue(bodyStr.contains(""""metadata":"""), "Body should contain metadata")
        assertTrue(bodyStr.contains(""""campaign":"welcome""""), "Body should contain metadata values")
    }

    @Test
    fun `createImageToken uses AuthMode OWNER`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_IMAGE_TOKEN)

        service.createImageToken()
        val request = server.takeRequest()

        val authHeader = request.getHeader("Authorization")
        assertNotNull(authHeader, "Authorization header should be present")
        assertTrue(authHeader!!.contains(OWNER_TOKEN), "Should use owner token")
    }

    // ========================================================================
    // uploadImage
    // ========================================================================

    @Test
    fun `uploadImage targets POST apps-appId-routines-email-image-token`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        val imageData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        service.uploadImage("img-tok-1", imageData)
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/apps/$TEST_APP_ID/routines/email-image/img-tok-1", request.path)
    }

    @Test
    fun `uploadImage sends base64-encoded image data`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        val imageData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val expectedBase64 = java.util.Base64.getEncoder().encodeToString(imageData)
        service.uploadImage("img-tok-1", imageData)
        val request = server.takeRequest()

        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains(""""image_jpeg_base64":"$expectedBase64""""), "Body should contain base64-encoded image data")
    }

    @Test
    fun `uploadImage sends transform and query when provided`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        val imageData = byteArrayOf(0x00, 0x01, 0x02)
        service.uploadImage("img-tok-1", imageData, transform = "screenshot", query = "crop=top")
        val request = server.takeRequest()

        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains(""""transform":"screenshot""""), "Body should contain transform")
        assertTrue(bodyStr.contains(""""query":"crop=top""""), "Body should contain query")
    }

    @Test
    fun `uploadImage uses AuthMode OWNER`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        service.uploadImage("img-tok-1", byteArrayOf(0x00))
        val request = server.takeRequest()

        val authHeader = request.getHeader("Authorization")
        assertNotNull(authHeader, "Authorization header should be present")
        assertTrue(authHeader!!.contains(OWNER_TOKEN), "Should use owner token")
    }

    // ========================================================================
    // createTextToken
    // ========================================================================

    @Test
    fun `createTextToken targets POST apps-appId-routines-email-text-tokens`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_TEXT_TOKEN)

        val result = service.createTextToken()
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/apps/$TEST_APP_ID/routines/email-text/tokens", request.path)
        assertEquals("txt-tok-1", result.token)
        assertEquals("https://cdn.example.com/txt/txt-tok-1", result.textUrl)
        assertEquals(1749676000L, result.expiresAt)
    }

    @Test
    fun `createTextToken sends ttl_seconds and metadata when provided`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_TEXT_TOKEN)

        val metadata = buildJsonObject { put("type", "greeting") }
        service.createTextToken(ttlSeconds = 7200, metadata = metadata)
        val request = server.takeRequest()

        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains(""""ttl_seconds":7200"""), "Body should contain ttl_seconds")
        assertTrue(bodyStr.contains(""""type":"greeting""""), "Body should contain metadata values")
    }

    @Test
    fun `createTextToken uses AuthMode OWNER`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_TEXT_TOKEN)

        service.createTextToken()
        val request = server.takeRequest()

        val authHeader = request.getHeader("Authorization")
        assertNotNull(authHeader, "Authorization header should be present")
        assertTrue(authHeader!!.contains(OWNER_TOKEN), "Should use owner token")
    }

    // ========================================================================
    // uploadText
    // ========================================================================

    @Test
    fun `uploadText targets POST apps-appId-routines-email-text-token`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        service.uploadText("txt-tok-1", "Hello, welcome to our service!")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/apps/$TEST_APP_ID/routines/email-text/txt-tok-1", request.path)
    }

    @Test
    fun `uploadText sends sentence field not text`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        service.uploadText("txt-tok-1", "Hello world")
        val request = server.takeRequest()

        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains(""""sentence":"Hello world""""), "Body should use 'sentence' field")
        assertFalse(bodyStr.contains(""""text":"""), "Body should NOT use 'text' field")
    }

    @Test
    fun `uploadText sends metadata when provided`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        val metadata = buildJsonObject { put("lang", "en") }
        service.uploadText("txt-tok-1", "Hi", metadata = metadata)
        val request = server.takeRequest()

        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains(""""sentence":"Hi""""), "Body should contain sentence")
        assertTrue(bodyStr.contains(""""lang":"en""""), "Body should contain metadata")
    }

    @Test
    fun `uploadText uses AuthMode OWNER`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue204()

        service.uploadText("txt-tok-1", "Test")
        val request = server.takeRequest()

        val authHeader = request.getHeader("Authorization")
        assertNotNull(authHeader, "Authorization header should be present")
        assertTrue(authHeader!!.contains(OWNER_TOKEN), "Should use owner token")
    }

    // ========================================================================
    // getTokenStatus
    // ========================================================================

    @Test
    fun `getTokenStatus targets GET apps-appId-routines-email-status-token`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_TOKEN_STATUS)

        val result = service.getTokenStatus("img-tok-1")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/apps/$TEST_APP_ID/routines/email-status/img-tok-1", request.path)
    }

    @Test
    fun `getTokenStatus deserializes all fields`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_TOKEN_STATUS)

        val result = service.getTokenStatus("img-tok-1")

        assertEquals("img-tok-1", result.token)
        assertEquals("image", result.type)
        assertEquals("ready", result.state)
        assertEquals(1741900000L, result.readyAt)
        assertNull(result.consumedAt)
        assertEquals(1749676000L, result.expiresAt)
        assertEquals(1741900100L, result.updatedAt)
    }

    @Test
    fun `getTokenStatus uses AuthMode OWNER`() = runTest {
        val http = createHttpClient()
        val service = EmailService(http)
        enqueue(FIXTURE_TOKEN_STATUS)

        service.getTokenStatus("img-tok-1")
        val request = server.takeRequest()

        val authHeader = request.getHeader("Authorization")
        assertNotNull(authHeader, "Authorization header should be present")
        assertTrue(authHeader!!.contains(OWNER_TOKEN), "Should use owner token")
    }
}
