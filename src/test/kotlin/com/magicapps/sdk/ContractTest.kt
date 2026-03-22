package com.magicapps.sdk

import com.magicapps.sdk.core.*
import com.magicapps.sdk.services.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Contract validation tests for the Kotlin SDK.
 *
 * These tests verify that every SDK service method:
 *   1) Targets the correct URL path (matching real Lambda handler routes)
 *   2) Uses the correct HTTP method (GET, POST, PUT, DELETE)
 *   3) Response data classes can deserialize realistic API response JSON
 *
 * All mock fixtures are golden fixtures sourced from real Lambda handler return
 * statements. Each fixture includes a source comment referencing the Lambda file,
 * function name, and approximate line number.
 *
 * Uses OkHttp MockWebServer to intercept HttpURLConnection requests
 * made by SdkHttpClient, which uses java.net.HttpURLConnection internally.
 */
class ContractTest {

    companion object {
        private lateinit var server: MockWebServer
        private lateinit var baseUrl: String
        private const val TEST_APP_ID = "test-app"

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = MockWebServer()
            server.start()
            baseUrl = server.url("/").toString().trimEnd('/')
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.shutdown()
        }

        // ====================================================================
        // Golden Fixtures (sourced from real Lambda handler return statements)
        // ====================================================================

        // Source: lambda/templates/index.js ok() helper (~line 1028)
        // Ping returns { message, requestId }
        const val FIXTURE_PING = """{"message":"pong","requestId":"req-abc123"}"""

        // Source: lambda/templates/index.js handleGet (~line 880) - returns single template
        const val FIXTURE_TEMPLATE = """{"template_id":"t1","app_id":"test-app","template_name":"Test","description":"A test template","created_at":1735689600,"updated_at":1735689600}"""

        // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
        // All AI responses are normalized to { id, provider, model, choices, usage }
        const val FIXTURE_CHAT_COMPLETION = """{"id":"ai_resp_abc123","provider":"openai","model":"gpt-4","choices":[{"index":0,"message":{"role":"assistant","content":"Hello!"},"finish_reason":"stop"}],"usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15,"estimated_cost_usd":0.001}}"""

        // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
        const val FIXTURE_EMBEDDING = """{"id":"ai_resp_emb123","provider":"openai","model":"text-embedding-3-small","choices":[],"usage":{"input_tokens":8,"output_tokens":0,"total_tokens":8,"estimated_cost_usd":0.0001},"data":[{"embedding":[0.1,0.2,0.3],"index":0}]}"""

        // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
        const val FIXTURE_IMAGE_GENERATION = """{"id":"ai_resp_img123","provider":"openai","model":"dall-e-3","choices":[],"usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0,"estimated_cost_usd":0.04},"data":[{"url":"https://example.com/generated.png","revised_prompt":"A friendly cat"}]}"""

        // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
        const val FIXTURE_MODERATION = """{"id":"ai_resp_mod123","provider":"openai","model":"text-moderation-latest","choices":[],"usage":{"input_tokens":5,"output_tokens":0,"total_tokens":5,"estimated_cost_usd":0.0},"results":[{"flagged":false,"categories":{"hate":false,"sexual":false,"violence":false},"category_scores":{"hate":0.001,"sexual":0.0002,"violence":0.0001}}]}"""

        // Source: lambda/devices/index.js (~line 22-26)
        // Returns { items: Device[] }
        const val FIXTURE_DEVICES = """{"items":[{"id":"d1","device_name":"TestDevice","display_name":"Test Device","device_type":"bluetooth","tags":["ios"],"os":"iOS","manufacturer":"Apple"}],"count":1}"""

        // Source: lambda/endpoints/index.js handleCreate (~line 221-232)
        const val FIXTURE_ENDPOINT_CREATED = """{"slug":"abc123","status":"active","expires_at":1749676000,"endpoint_path":"/events/abc123","hmac_secret":"secret-key","hmac_required":true}"""

        // Source: lambda/endpoints/index.js handleRevokeAndReplace (~line 402-414)
        const val FIXTURE_ENDPOINT_REVOKE_AND_REPLACE = """{"old_slug":"old-slug","new_slug":"new-slug","new_endpoint_path":"/events/new-slug","revoked_expires_at":1741900000,"new_expires_at":1749676000,"hmac_secret":"new-hmac-secret","hmac_required":true}"""

        // Source: lambda/endpoints/index.js handleRevoke (~line 521-524)
        const val FIXTURE_ENDPOINT_REVOKE = """{"slug":"revoked-slug","revoked":true}"""

        // Source: lambda/events/index.js POST handler (~line 238-246)
        const val FIXTURE_POST_EVENT = """{"slug":"my-slug","timestamp":1741900000,"expires_at":1749676000}"""

        // Source: lambda/events/index.js GET handler with data (~line 250-260)
        const val FIXTURE_CONSUME_EVENT = """{"slug":"my-slug","timestamp":1741900000,"created_at":1741899000,"expires_at":1749676000,"text":"dictated text","keywords":["hello","world"],"raw_text":"dictated text full","empty":false}"""

        // Source: lambda/events/index.js GET handler empty slot (~line 262-267)
        const val FIXTURE_CONSUME_EVENT_EMPTY = """{"slug":"my-slug","empty":true,"text":"George Lucas"}"""

        // Source: lambda/lookup_tables/index.js toSummary (~line 867-880)
        // list handler returns { items: LookupTableSummary[] }
        const val FIXTURE_LOOKUP_TABLES_LIST = """{"items":[{"lookup_table_id":"lt1","name":"Cities","description":"World cities","schema_keys":["name","country"],"schema_key_count":2,"schema_keys_truncated":false,"version":1,"payload_hash":"abc123","storage_mode":"chunked","chunk_count":3,"updated_at":1741900000}]}"""

        // Source: lambda/lookup_tables/index.js toClientDetail (~line 903-920)
        const val FIXTURE_LOOKUP_TABLE_DETAIL = """{"lookup_table_id":"lt1","name":"Cities","description":"World cities","schema_keys":["name","country"],"schema_key_count":2,"schema_keys_truncated":false,"version":2,"payload_hash":"abc123","storage_mode":"chunked","chunk_count":3,"updated_at":1741900000,"prompt":"Find cities by name","default_success_sentence":"Found {{name}} in {{country}}","default_fail_sentence":"City not found","chunk_encoding":"json","manifest_hash":"mhash","chunks":[{"index":0,"path":"/chunks/0","sha256":"hash0","byte_length":1024},{"index":1,"path":"/chunks/1","sha256":"hash1","byte_length":512}]}"""

        // Source: lambda/lookup_tables/index.js handleClientChunk (~line 134-138)
        const val FIXTURE_LOOKUP_TABLE_CHUNK = """{"London":{"country":"UK","population":9000000}}"""
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Create a fresh SdkHttpClient pointing at MockWebServer. */
    private fun createHttpClient(
        accessToken: String? = "test-access-token",
        ownerToken: String? = "test-owner-token"
    ): SdkHttpClient {
        val config = SdkConfig(
            baseUrl = baseUrl,
            appId = TEST_APP_ID,
            accessToken = accessToken,
            ownerToken = ownerToken,
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
    // AuthService contract tests
    // ========================================================================

    @Nested
    inner class AuthServiceContract {
        @Test
        fun `refreshToken targets POST auth-client-refresh`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"accessToken":"new-tok","refreshToken":"new-rt","expiresIn":3600}""")

            val result = service.refreshToken("old-rt")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/refresh", request.path)
            assertEquals("new-tok", result.accessToken)
        }

        @Test
        fun `linkProvider targets POST auth-client-link`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"success":true,"linkedProviders":["google","apple"]}""")

            val result = service.linkProvider("google", "google-token")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/link", request.path)
            assertTrue(result.success)
            assertEquals(listOf("google", "apple"), result.linkedProviders)
        }

        @Test
        fun `getPasskeyRegisterOptions targets POST auth-client-passkey-register-options`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{
                "challenge":"abc123",
                "rp":{"id":"example.com","name":"Example"},
                "user":{"id":"u1","name":"user@example.com","displayName":"User"},
                "timeout":60000
            }""")

            val result = service.getPasskeyRegisterOptions()
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/passkey/register/options", request.path)
            assertEquals("abc123", result.challenge)
        }

        @Test
        fun `verifyPasskeyRegistration targets POST auth-client-passkey-register-verify`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"success":true,"credentialId":"cred-1"}""")

            val result = service.verifyPasskeyRegistration("""{"id":"test"}""")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/passkey/register/verify", request.path)
            assertTrue(result.success)
        }

        @Test
        fun `getPasskeyAuthOptions targets POST auth-client-passkey-authenticate-options`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"challenge":"xyz","timeout":60000,"rpId":"example.com","userVerification":"preferred"}""")

            val result = service.getPasskeyAuthOptions()
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/passkey/authenticate/options", request.path)
            assertEquals("xyz", result.challenge)
        }

        @Test
        fun `verifyPasskeyAuth targets POST auth-client-passkey-authenticate-verify`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"accessToken":"pk-tok","refreshToken":"pk-rt"}""")

            val result = service.verifyPasskeyAuth("""{"id":"test"}""")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/passkey/authenticate/verify", request.path)
            assertEquals("pk-tok", result.accessToken)
        }

        @Test
        fun `requestEmailMagicLink targets POST auth-client-email-request`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"success":true,"message":"Magic link sent"}""")

            val result = service.requestEmailMagicLink("user@example.com")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/email/request", request.path)
            assertTrue(result.success)
        }

        @Test
        fun `verifyEmailMagicLink targets POST auth-client-email-verify`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"accessToken":"ml-tok","refreshToken":"ml-rt"}""")

            val result = service.verifyEmailMagicLink("magic-token")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/email/verify", request.path)
            assertEquals("ml-tok", result.accessToken)
        }
    }

    // ========================================================================
    // GoogleAuthService contract tests
    // ========================================================================

    @Nested
    inner class GoogleAuthServiceContract {
        @Test
        fun `exchangeToken targets POST auth-client-google-exchange`() = runTest {
            val http = createHttpClient()
            val service = GoogleAuthService(http)
            enqueue("""{"accessToken":"g-tok","refreshToken":"g-rt","isNewUser":true}""")

            val result = service.exchangeToken("google-id-token", "google-access-token")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/client/google/exchange", request.path)
            assertEquals("g-tok", result.accessToken)
            assertEquals(true, result.isNewUser)
        }
    }

    // ========================================================================
    // AiService contract tests
    // ========================================================================

    @Nested
    inner class AiServiceContract {
        @Test
        fun `createChatCompletion targets POST apps-appId-ai-chat-completions`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            enqueue(FIXTURE_CHAT_COMPLETION)

            val request = ChatCompletionRequest(
                messages = listOf(ChatMessage(role = "user", content = "Hi")),
                model = "gpt-4"
            )
            val result = service.createChatCompletion(request)
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/chat/completions", recorded.path)
            assertEquals(1, result.choices.size)
            assertEquals("Hello!", result.choices[0].message.content)
        }

        @Test
        fun `chat convenience method targets same endpoint`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            enqueue(FIXTURE_CHAT_COMPLETION)

            service.chat("Hello", "gpt-4")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/chat/completions", recorded.path)
        }

        @Test
        fun `createEmbedding targets POST apps-appId-ai-embeddings`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            enqueue(FIXTURE_EMBEDDING)

            val result = service.createEmbedding("test input", "text-embedding-3-small")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/embeddings", recorded.path)
            assertEquals(3, result.data[0].embedding.size)
        }

        @Test
        fun `createImage targets POST apps-appId-ai-images-generations`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            enqueue(FIXTURE_IMAGE_GENERATION)

            val result = service.createImage("a cat", n = 1, size = "1024x1024")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/images/generations", recorded.path)
            assertEquals(1, result.data.size)
            assertEquals("https://example.com/generated.png", result.data[0].url)
        }

        @Test
        fun `createModeration targets POST apps-appId-ai-moderations`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            enqueue(FIXTURE_MODERATION)

            val result = service.createModeration("safe text")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/moderations", recorded.path)
            assertFalse(result.results[0].flagged)
        }

    }

    // ========================================================================
    // TemplatesService contract tests
    // ========================================================================

    @Nested
    inner class TemplatesServiceContract {
        @Test
        fun `get targets GET apps-appId-templates-templateId`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            // Source: lambda/templates/index.js handleGet (~line 880) - returns single template
            enqueue(FIXTURE_TEMPLATE)

            val result = service.get("t1")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/templates/t1", recorded.path)
            assertEquals("Test", result.templateName)
        }

        @Test
        fun `getCatalog targets GET apps-appId-catalog`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{"apps":[],"templates":[]}""")

            val result = service.getCatalog()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/catalog", recorded.path)
            assertTrue(result.containsKey("apps"))
        }
    }

    // ========================================================================
    // DevicesService contract tests
    // ========================================================================

    @Nested
    inner class DevicesServiceContract {
        @Test
        fun `list targets GET apps-appId-devices`() = runTest {
            val http = createHttpClient()
            val service = DevicesService(http)
            // Source: lambda/devices/index.js (~line 22-26) - returns { items: Device[] }
            enqueue(FIXTURE_DEVICES)

            val result = service.list()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/devices", recorded.path)
            assertEquals(1, result.allDevices.size)
            assertEquals("d1", result.allDevices[0].id)
        }

        @Test
        fun `getAll returns flat list of devices`() = runTest {
            val http = createHttpClient()
            val service = DevicesService(http)
            // Source: lambda/devices/index.js (~line 22-26) - returns { items: Device[] }
            enqueue(FIXTURE_DEVICES)

            val result = service.getAll()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/devices", recorded.path)
            assertEquals(1, result.size)
            assertEquals("TestDevice", result[0].deviceName)
        }
    }

    // ========================================================================
    // EndpointsService contract tests
    // ========================================================================

    @Nested
    inner class EndpointsServiceContract {
        @Test
        fun `create targets POST apps-appId-endpoints`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/endpoints/index.js handleCreate (~line 221-232)
            enqueue(FIXTURE_ENDPOINT_CREATED)

            val result = service.create()
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/endpoints", recorded.path)
            assertEquals("abc123", result.slug)
            assertEquals("secret-key", result.hmacSecret)
            assertEquals(true, result.hmacRequired)
        }

        @Test
        fun `revokeAndReplace targets POST apps-appId-endpoints-revoke_and_replace`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/endpoints/index.js handleRevokeAndReplace (~line 402-414)
            enqueue(FIXTURE_ENDPOINT_REVOKE_AND_REPLACE)

            val result = service.revokeAndReplace("old-slug")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/endpoints/revoke_and_replace", recorded.path)
            assertEquals("old-slug", result.oldSlug)
            assertEquals("new-slug", result.newSlug)
        }

        @Test
        fun `revoke targets POST apps-appId-endpoints-revoke`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/endpoints/index.js handleRevoke (~line 521-524)
            enqueue(FIXTURE_ENDPOINT_REVOKE)

            val result = service.revoke("revoked-slug")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/endpoints/revoke", recorded.path)
            assertTrue(result.revoked)
        }

        @Test
        fun `postEvent targets POST events-slug`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/events/index.js POST handler (~line 238-246)
            enqueue(FIXTURE_POST_EVENT)

            val result = service.postEvent("my-slug", """{"text":"hello"}""")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/events/my-slug", recorded.path)
            assertEquals("my-slug", result.slug)
        }

        @Test
        fun `postEvent with HMAC includes signature headers`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/events/index.js POST handler (~line 238-246)
            enqueue(FIXTURE_POST_EVENT)

            service.postEvent("my-slug", """{"text":"hello"}""", hmacSecret = "secret")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/events/my-slug", recorded.path)
            assertNotNull(recorded.getHeader("X-Signature"), "HMAC signature header should be present")
            assertNotNull(recorded.getHeader("X-Timestamp"), "HMAC timestamp header should be present")
        }

        @Test
        fun `consumeEvent targets GET events-slug`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/events/index.js GET handler with data (~line 250-260)
            enqueue(FIXTURE_CONSUME_EVENT)

            val result = service.consumeEvent("my-slug")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/events/my-slug", recorded.path)
            assertEquals("dictated text", result.text)
            assertEquals(listOf("hello", "world"), result.keywords)
        }
    }

    // ========================================================================
    // LookupTablesService contract tests
    // ========================================================================

    @Nested
    inner class LookupTablesServiceContract {
        @Test
        fun `list targets GET lookup-tables`() = runTest {
            val http = createHttpClient()
            val service = LookupTablesService(http)
            // Source: lambda/lookup_tables/index.js list handler with toSummary (~line 867-880)
            enqueue(FIXTURE_LOOKUP_TABLES_LIST)

            val result = service.list()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/lookup-tables", recorded.path)
            assertEquals(1, result.items.size)
            assertEquals("lt1", result.items[0].lookupTableId)
        }

        @Test
        fun `get targets GET lookup-tables-lookupTableId`() = runTest {
            val http = createHttpClient()
            val service = LookupTablesService(http)
            // Source: lambda/lookup_tables/index.js toClientDetail (~line 903-920)
            enqueue(FIXTURE_LOOKUP_TABLE_DETAIL)

            val result = service.get("lt1")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/lookup-tables/lt1", recorded.path)
            assertEquals("Cities", result.name)
            assertEquals(2, result.chunks?.size)
        }

        @Test
        fun `get URL-encodes lookup table IDs with special characters`() = runTest {
            val http = createHttpClient()
            val service = LookupTablesService(http)
            // Source: lambda/lookup_tables/index.js toClientDetail (~line 903-920)
            enqueue("""{
                "lookup_table_id":"lt/special",
                "name":"Special",
                "version":1,
                "storage_mode":"chunked",
                "chunk_count":0,
                "updated_at":0,
                "chunks":[]
            }""")

            service.get("lt/special")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            // URL-encoded slash
            assertTrue(recorded.path!!.contains("lt%2Fspecial"))
        }

        @Test
        fun `getChunk targets GET lookup-tables-id-chunks-index`() = runTest {
            val http = createHttpClient()
            val service = LookupTablesService(http)
            // Source: lambda/lookup_tables/index.js handleClientChunk (~line 134-138)
            enqueue(FIXTURE_LOOKUP_TABLE_CHUNK)

            val result = service.getChunk("lt1", 0)
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/lookup-tables/lt1/chunks/0", recorded.path)
            assertTrue(result.containsKey("London"))
        }

        @Test
        fun `getChunk with version includes version query param`() = runTest {
            val http = createHttpClient()
            val service = LookupTablesService(http)
            // Source: lambda/lookup_tables/index.js handleClientChunk (~line 134-138)
            enqueue("""{"Paris":{"country":"FR"}}""")

            service.getChunk("lt1", 1, version = 3)
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertTrue(recorded.path!!.startsWith("/lookup-tables/lt1/chunks/1"))
            assertTrue(recorded.path!!.contains("version=3"))
        }
    }

    // ========================================================================
    // MagicAppsClient (ping) contract test
    // ========================================================================

    @Nested
    inner class MagicAppsClientContract {
        @Test
        fun `ping targets GET ping`() = runTest {
            val config = SdkConfig(
                baseUrl = baseUrl,
                appId = TEST_APP_ID,
                retries = 0,
                tokenStorage = InMemoryTokenStorage()
            )
            val client = MagicAppsClient(config)
            // Source: lambda/templates/index.js ok() helper (~line 1028)
            enqueue(FIXTURE_PING)

            val result = client.ping()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/ping", recorded.path)
            assertEquals("pong", result.message)
        }
    }

    // ========================================================================
    // OwnerService contract tests
    // ========================================================================

    @Nested
    inner class OwnerServiceContract {
        @Test
        fun `registerOwner targets POST owner-register`() = runTest {
            val http = createHttpClient()
            val service = OwnerService(http)
            enqueue("""{"owner_id":"own-1","app_id":"test-app","token":"owner-tok","status":"created"}""")

            val result = service.registerOwner("device-123", "test-app")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/owner/register", recorded.path)
            assertEquals("own-1", result.ownerId)
            assertEquals("test-app", result.appId)
            assertEquals("owner-tok", result.token)
        }

        @Test
        fun `registerOwner includes hcaptcha token when provided`() = runTest {
            val http = createHttpClient()
            val service = OwnerService(http)
            enqueue("""{"owner_id":"own-2","app_id":"test-app","status":"created"}""")

            service.registerOwner("device-456", "test-app", hcaptchaToken = "captcha-tok")
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""hcaptcha_token":"captcha-tok""""))
        }

        @Test
        fun `migrateOwnerToUser targets POST owner-migrate`() = runTest {
            val http = createHttpClient()
            val service = OwnerService(http)
            enqueue("""{"owner_id":"own-1","app_id":"test-app","user_id":"user-1","status":"migrated"}""")

            val result = service.migrateOwnerToUser("own-1", "test-app")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/owner/migrate", recorded.path)
            assertEquals("own-1", result.ownerId)
            assertEquals("user-1", result.userId)
            assertEquals("migrated", result.status)
        }
    }

    // ========================================================================
    // SettingsService contract tests
    // ========================================================================

    @Nested
    inner class SettingsServiceContract {
        @Test
        fun `getSettings targets GET apps-appId-settings`() = runTest {
            val http = createHttpClient()
            val service = SettingsService(http)
            enqueue("""{"theme":"dark","notifications":true}""")

            val result = service.getSettings()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/settings", recorded.path)
            assertTrue(result.containsKey("theme"))
        }

        @Test
        fun `updateSettings targets PUT apps-appId-settings`() = runTest {
            val http = createHttpClient()
            val service = SettingsService(http)
            enqueue("""{"theme":"light","notifications":false}""")

            val body = kotlinx.serialization.json.Json.parseToJsonElement("""{"theme":"light"}""").jsonObject
            val result = service.updateSettings(body)
            val recorded = server.takeRequest()

            assertEquals("PUT", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/settings", recorded.path)
            assertTrue(result.containsKey("theme"))
        }

        @Test
        fun `getConfig targets GET apps-appId-config`() = runTest {
            val http = createHttpClient()
            val service = SettingsService(http)
            enqueue("""{"feature_flags":{"ai_enabled":true}}""")

            val result = service.getConfig()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/config", recorded.path)
            assertTrue(result.containsKey("feature_flags"))
        }

        @Test
        fun `updateConfig targets PUT apps-appId-config`() = runTest {
            val http = createHttpClient()
            val service = SettingsService(http)
            enqueue("""{"feature_flags":{"ai_enabled":false}}""")

            val body = kotlinx.serialization.json.Json.parseToJsonElement("""{"feature_flags":{"ai_enabled":false}}""").jsonObject
            val result = service.updateConfig(body)
            val recorded = server.takeRequest()

            assertEquals("PUT", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/config", recorded.path)
        }

        @Test
        fun `getIntegrationSecret targets GET apps-appId-integrations-id-secret`() = runTest {
            val http = createHttpClient()
            val service = SettingsService(http)
            enqueue("""{"api_key":"sk-secret-123"}""")

            val result = service.getIntegrationSecret("int-1")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/integrations/int-1/secret", recorded.path)
            assertTrue(result.containsKey("api_key"))
        }

        @Test
        fun `uploadIntegrationSecret targets POST apps-appId-integrations-id-secret`() = runTest {
            val http = createHttpClient()
            val service = SettingsService(http)
            enqueue("""{"status":"saved"}""")

            val body = kotlinx.serialization.json.Json.parseToJsonElement("""{"api_key":"sk-new-secret"}""").jsonObject
            val result = service.uploadIntegrationSecret("int-1", body)
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/integrations/int-1/secret", recorded.path)
        }
    }

    // ========================================================================
    // MagicAppsClient.getAppInfo contract test
    // ========================================================================

    @Nested
    inner class GetAppInfoContract {
        @Test
        fun `getAppInfo targets GET apps-appId`() = runTest {
            val config = SdkConfig(
                baseUrl = baseUrl,
                appId = TEST_APP_ID,
                retries = 0,
                tokenStorage = InMemoryTokenStorage()
            )
            val client = MagicAppsClient(config)
            enqueue("""{"app_id":"test-app","name":"Test App","display_name":"Test","description":"A test app","status":"active","icon_url":"https://example.com/icon.png","category":"tools"}""")

            val result = client.getAppInfo()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID", recorded.path)
            assertEquals("test-app", result.appId)
            assertEquals("Test App", result.name)
            assertEquals("Test", result.displayName)
            assertEquals("active", result.status)
        }
    }

    // ========================================================================
    // Response deserialization robustness tests (golden fixture shape validation)
    // ========================================================================

    @Nested
    inner class ResponseDeserializationContract {
        @Test
        fun `ChatCompletionResponse handles normalized Lambda response shape`() {
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            // Real Lambda returns { id, provider, model, choices, usage } with usage containing
            // input_tokens/output_tokens/total_tokens/estimated_cost_usd
            val result = json.decodeFromString<ChatCompletionResponse>(FIXTURE_CHAT_COMPLETION)
            assertEquals(1, result.choices.size)
            assertEquals("Hello!", result.choices[0].message.content)
            assertEquals("stop", result.choices[0].finishReason)
        }

        @Test
        fun `DeviceCatalogResponse handles items field from real Lambda`() {
            // Source: lambda/devices/index.js (~line 22-26) - returns { items: [] }
            val result = json.decodeFromString<DeviceCatalogResponse>(FIXTURE_DEVICES)
            assertEquals(1, result.allDevices.size)
            assertEquals("TestDevice", result.allDevices[0].deviceName)
        }

        @Test
        fun `DeviceCatalogResponse handles legacy devices field`() {
            val devicesResult = json.decodeFromString<DeviceCatalogResponse>(
                """{"devices":[{"device_name":"Dev2"}],"count":1}"""
            )
            assertEquals(1, devicesResult.allDevices.size)
        }

        @Test
        fun `ConsumedEvent handles empty event from real Lambda`() {
            // Source: lambda/events/index.js GET handler empty slot (~line 262-267)
            val result = json.decodeFromString<ConsumedEvent>(FIXTURE_CONSUME_EVENT_EMPTY)
            assertEquals(true, result.empty)
            assertEquals("George Lucas", result.text)
        }

        @Test
        fun `LookupTableDetail handles full manifest response from real Lambda`() {
            // Source: lambda/lookup_tables/index.js toClientDetail (~line 903-920)
            val result = json.decodeFromString<LookupTableDetail>(FIXTURE_LOOKUP_TABLE_DETAIL)
            assertEquals(2, result.version)
            assertEquals(2, result.chunks?.size)
            assertEquals("Found {{name}} in {{country}}", result.defaultSuccessSentence)
        }

        @Test
        fun `EmbeddingResponse deserializes normalized Lambda response`() {
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            val result = json.decodeFromString<EmbeddingResponse>(FIXTURE_EMBEDDING)
            assertEquals(3, result.data[0].embedding.size)
            assertEquals(0.2, result.data[0].embedding[1])
        }

        @Test
        fun `ImageGenerationResponse handles b64_json format`() {
            val result = json.decodeFromString<ImageGenerationResponse>("""{
                "created":1700000000,
                "data":[{"b64_json":"base64data","revised_prompt":"A revised prompt"}]
            }""")
            assertEquals("base64data", result.data[0].b64Json)
            assertNull(result.data[0].url)
        }

        @Test
        fun `ModerationResponse handles minimal response`() {
            val result = json.decodeFromString<ModerationResponse>("""{
                "results":[{"flagged":true}]
            }""")
            assertTrue(result.results[0].flagged)
            assertNull(result.results[0].categories)
        }

        @Test
        fun `CreateEndpointResponse deserializes real Lambda shape`() {
            // Source: lambda/endpoints/index.js handleCreate (~line 221-232)
            val result = json.decodeFromString<CreateEndpointResponse>(FIXTURE_ENDPOINT_CREATED)
            assertEquals("abc123", result.slug)
            assertEquals("secret-key", result.hmacSecret)
            assertEquals(true, result.hmacRequired)
        }

        @Test
        fun `RevokeAndReplaceResponse deserializes real Lambda shape`() {
            // Source: lambda/endpoints/index.js handleRevokeAndReplace (~line 402-414)
            val result = json.decodeFromString<RevokeAndReplaceResponse>(FIXTURE_ENDPOINT_REVOKE_AND_REPLACE)
            assertEquals("old-slug", result.oldSlug)
            assertEquals("new-hmac-secret", result.hmacSecret)
        }

    }

    // ========================================================================
    // HMAC utility contract tests
    // ========================================================================

    @Nested
    inner class HmacContract {
        @Test
        fun `generateHmacSignature produces deterministic output`() {
            val sig1 = generateHmacSignature("slug", """{"text":"hello"}""", "secret", timestampSec = 1700000000)
            val sig2 = generateHmacSignature("slug", """{"text":"hello"}""", "secret", timestampSec = 1700000000)

            assertEquals(sig1.signature, sig2.signature)
            assertEquals("1700000000", sig1.timestamp)
        }

        @Test
        fun `verifyHmacSignature validates correct signature`() {
            val body = """{"text":"hello"}"""
            val secret = "test-secret"
            val slug = "my-slug"
            val sig = generateHmacSignature(slug, body, secret, timestampSec = System.currentTimeMillis() / 1000)

            assertTrue(verifyHmacSignature(slug, body, sig.signature, sig.timestamp, secret))
        }

        @Test
        fun `verifyHmacSignature rejects wrong signature`() {
            assertFalse(
                verifyHmacSignature("slug", "body", "wrong-sig", (System.currentTimeMillis() / 1000).toString(), "secret")
            )
        }

        @Test
        fun `verifyHmacSignature rejects expired timestamp`() {
            val oldTimestamp = ((System.currentTimeMillis() / 1000) - 600).toString()
            val sig = generateHmacSignature("slug", "body", "secret", timestampSec = oldTimestamp.toLong())

            assertFalse(verifyHmacSignature("slug", "body", sig.signature, oldTimestamp, "secret", maxSkewSeconds = 300))
        }
    }

    // ========================================================================
    // Request body contract tests
    // ========================================================================

    @Nested
    inner class RequestBodyContract {
        @Test
        fun `endpoint revoke sends slug in body`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            // Source: lambda/endpoints/index.js handleRevoke (~line 521-524)
            enqueue(FIXTURE_ENDPOINT_REVOKE)

            service.revoke("s1")
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""slug":"s1""""))
        }

        @Test
        fun `AI chat completion sends correct request structure`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            // Source: lambda/ai_proxy/index.js normalizeProviderResponse (~line 830-874)
            enqueue(FIXTURE_CHAT_COMPLETION)

            service.createChatCompletion(
                ChatCompletionRequest(
                    messages = listOf(ChatMessage("user", "hello")),
                    model = "gpt-4",
                    temperature = 0.7,
                    maxTokens = 100
                )
            )
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""role":"user""""))
            assertTrue(body.contains(""""content":"hello""""))
            assertTrue(body.contains(""""model":"gpt-4""""))
        }
    }

    // ========================================================================
    // Auth header contract tests
    // ========================================================================

    @Nested
    inner class AuthHeaderContract {
        @Test
        fun `BEARER endpoints include Authorization header`() = runTest {
            val http = createHttpClient(accessToken = "my-bearer-token")
            val service = SettingsService(http)
            enqueue("""{"key":"value"}""")

            service.getSettings()
            val recorded = server.takeRequest()

            val authHeader = recorded.getHeader("Authorization")
            assertNotNull(authHeader)
            assertTrue(authHeader!!.startsWith("Bearer "))
        }

        @Test
        fun `NONE endpoints do not include Authorization header`() = runTest {
            val http = createHttpClient(accessToken = null, ownerToken = null)
            val service = TemplatesService(http)
            enqueue(FIXTURE_TEMPLATE)

            service.get("t1")
            val recorded = server.takeRequest()

            assertNull(recorded.getHeader("Authorization"))
        }

        @Test
        fun `OWNER endpoints include owner token`() = runTest {
            val http = createHttpClient(ownerToken = "my-owner-token")
            val service = EndpointsService(http)
            // Source: lambda/endpoints/index.js handleCreate (~line 221-232)
            enqueue(FIXTURE_ENDPOINT_CREATED)

            service.create()
            val recorded = server.takeRequest()

            val authHeader = recorded.getHeader("Authorization")
            assertNotNull(authHeader)
            assertTrue(authHeader!!.contains("my-owner-token"))
        }

        @Test
        fun `all requests include X-App-Id header`() = runTest {
            val http = createHttpClient()
            val service = DevicesService(http)
            // Source: lambda/devices/index.js (~line 22-26)
            enqueue("""{"items":[]}""")

            service.list()
            val recorded = server.takeRequest()

            assertEquals(TEST_APP_ID, recorded.getHeader("X-App-Id"))
        }
    }
}
