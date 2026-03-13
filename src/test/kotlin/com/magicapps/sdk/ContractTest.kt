package com.magicapps.sdk

import com.magicapps.sdk.core.*
import com.magicapps.sdk.services.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
        fun `login targets POST auth-login`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"accessToken":"tok","refreshToken":"rt","idToken":"id","expiresIn":3600}""")

            val result = service.login("user@example.com", "password")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/login", request.path)
            assertEquals("tok", result.accessToken)
        }

        @Test
        fun `register targets POST auth-register`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"userId":"u1","email":"user@example.com","confirmed":false}""")

            val result = service.register("user@example.com", "password", "Test User")
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/register", request.path)
            assertEquals("u1", result.userId)
            assertFalse(result.confirmed)
        }

        @Test
        fun `logout targets POST auth-logout`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue204()

            service.logout()
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/auth/logout", request.path)
        }

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
            enqueue("""{
                "id":"chatcmpl-1",
                "object":"chat.completion",
                "created":1700000000,
                "model":"gpt-4",
                "choices":[{"index":0,"message":{"role":"assistant","content":"Hello!"},"finish_reason":"stop"}],
                "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}
            }""")

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
            enqueue("""{
                "choices":[{"index":0,"message":{"role":"assistant","content":"World"},"finish_reason":"stop"}]
            }""")

            service.chat("Hello", "gpt-4")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/chat/completions", recorded.path)
        }

        @Test
        fun `createEmbedding targets POST apps-appId-ai-embeddings`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            enqueue("""{
                "object":"list",
                "data":[{"object":"embedding","embedding":[0.1,0.2,0.3],"index":0}],
                "model":"text-embedding-ada-002",
                "usage":{"prompt_tokens":5,"total_tokens":5}
            }""")

            val result = service.createEmbedding("test input", "text-embedding-ada-002")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/embeddings", recorded.path)
            assertEquals(3, result.data[0].embedding.size)
        }

        @Test
        fun `createImage targets POST apps-appId-ai-images-generations`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            enqueue("""{
                "created":1700000000,
                "data":[{"url":"https://example.com/img.png","revised_prompt":"A cat"}]
            }""")

            val result = service.createImage("a cat", n = 1, size = "1024x1024")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/images/generations", recorded.path)
            assertEquals(1, result.data.size)
            assertEquals("https://example.com/img.png", result.data[0].url)
        }

        @Test
        fun `createModeration targets POST apps-appId-ai-moderations`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            enqueue("""{
                "id":"modr-1",
                "model":"text-moderation-latest",
                "results":[{"flagged":false,"categories":{"hate":false,"sexual":false},"category_scores":{"hate":0.01}}]
            }""")

            val result = service.createModeration("safe text")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/moderations", recorded.path)
            assertFalse(result.results[0].flagged)
        }

        @Test
        fun `getUsageSummary targets GET apps-appId-ai-usage-summary`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            enqueue("""{
                "total_requests":100,
                "total_tokens":50000,
                "total_cost":1.50,
                "period":"2026-03",
                "breakdown":[{"endpoint":"chat/completions","model":"gpt-4","requests":80,"tokens":40000,"cost":1.20}]
            }""")

            val result = service.getUsageSummary()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/ai/usage/summary", recorded.path)
            assertEquals(100, result.totalRequests)
        }
    }

    // ========================================================================
    // TemplatesService contract tests
    // ========================================================================

    @Nested
    inner class TemplatesServiceContract {
        @Test
        fun `list targets GET apps-appId-templates`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{
                "templates":[{"template_id":"t1","name":"Test","description":"A test template"}],
                "count":1
            }""")

            val result = service.list()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/templates", recorded.path)
            assertEquals(1, result.allTemplates.size)
            assertEquals("t1", result.allTemplates[0].templateId)
        }

        @Test
        fun `list with pagination targets GET apps-appId-templates with next_token query`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{"templates":[],"count":0}""")

            service.list(nextToken = "page2")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertTrue(recorded.path!!.startsWith("/apps/$TEST_APP_ID/templates"))
            assertTrue(recorded.path!!.contains("next_token=page2"))
        }

        @Test
        fun `get targets GET apps-appId-templates-templateId`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{"template_id":"t1","app_id":"$TEST_APP_ID","name":"Test"}""")

            val result = service.get("t1")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/templates/t1", recorded.path)
            assertEquals("Test", result.name)
        }

        @Test
        fun `create targets POST apps-appId-templates`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{"template_id":"t2","name":"New Template","description":"desc"}""")

            val result = service.create("New Template", description = "desc")
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/templates", recorded.path)
            assertEquals("t2", result.templateId)
        }

        @Test
        fun `update targets PUT apps-appId-templates-templateId`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{"template_id":"t1","name":"Updated"}""")

            val result = service.update("t1", name = "Updated")
            val recorded = server.takeRequest()

            assertEquals("PUT", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/templates/t1", recorded.path)
            assertEquals("Updated", result.name)
        }

        @Test
        fun `delete targets DELETE apps-appId-templates-templateId`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue204()

            service.delete("t1")
            val recorded = server.takeRequest()

            assertEquals("DELETE", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/templates/t1", recorded.path)
        }

        @Test
        fun `browseRegistry targets GET registry-apps`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{
                "apps":[{"app_id":"app1","name":"App One","slug":"app-one"}]
            }""")

            val result = service.browseRegistry()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/registry/apps", recorded.path)
            assertEquals(1, result.allApps.size)
            assertEquals("app1", result.allApps[0].appId)
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
            enqueue("""{
                "devices":[{"device_id":"d1","device_name":"TestDevice","display_name":"Test Device","device_type":"bluetooth"}],
                "count":1
            }""")

            val result = service.list()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/devices", recorded.path)
            assertEquals(1, result.allDevices.size)
            assertEquals("d1", result.allDevices[0].deviceId)
        }

        @Test
        fun `getAll returns flat list of devices`() = runTest {
            val http = createHttpClient()
            val service = DevicesService(http)
            enqueue("""{
                "devices":[
                    {"device_id":"d1","device_name":"Dev1"},
                    {"device_id":"d2","device_name":"Dev2"}
                ]
            }""")

            val result = service.getAll()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/devices", recorded.path)
            assertEquals(2, result.size)
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
            enqueue("""{
                "slug":"abc123",
                "status":"active",
                "expires_at":1700000000,
                "endpoint_path":"/events/abc123",
                "hmac_secret":"secret-key",
                "hmac_required":true
            }""")

            val result = service.create()
            val recorded = server.takeRequest()

            assertEquals("POST", recorded.method)
            assertEquals("/apps/$TEST_APP_ID/endpoints", recorded.path)
            assertEquals("abc123", result.slug)
            assertEquals("secret-key", result.hmacSecret)
        }

        @Test
        fun `revokeAndReplace targets POST apps-appId-endpoints-revoke_and_replace`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            enqueue("""{
                "old_slug":"old-slug",
                "new_slug":"new-slug",
                "new_endpoint_path":"/events/new-slug",
                "revoked_expires_at":1700000000,
                "new_expires_at":1700100000
            }""")

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
            enqueue("""{"slug":"revoked-slug","revoked":true}""")

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
            enqueue("""{"slug":"my-slug","timestamp":1700000000,"expires_at":1700100000}""")

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
            enqueue("""{"slug":"my-slug","timestamp":1700000000,"expires_at":1700100000}""")

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
            enqueue("""{
                "slug":"my-slug",
                "timestamp":1700000000,
                "created_at":1700000000,
                "expires_at":1700100000,
                "text":"dictated text",
                "keywords":["hello","world"],
                "empty":false
            }""")

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
            enqueue("""{
                "items":[{
                    "lookup_table_id":"lt1",
                    "name":"Cities",
                    "description":"World cities",
                    "schema_keys":["name","country"],
                    "schema_key_count":2,
                    "version":1,
                    "storage_mode":"chunked",
                    "chunk_count":3,
                    "updated_at":1700000000
                }]
            }""")

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
            enqueue("""{
                "lookup_table_id":"lt1",
                "name":"Cities",
                "version":2,
                "storage_mode":"chunked",
                "chunk_count":3,
                "updated_at":1700000000,
                "prompt":"Find cities by name",
                "default_success_sentence":"Found {{name}} in {{country}}",
                "chunk_encoding":"json",
                "manifest_hash":"abc123",
                "chunks":[
                    {"index":0,"path":"/chunks/0","sha256":"hash0","byte_length":1024},
                    {"index":1,"path":"/chunks/1","sha256":"hash1","byte_length":512}
                ]
            }""")

            val result = service.get("lt1")
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/lookup-tables/lt1", recorded.path)
            assertEquals("Cities", result.name)
            assertEquals(2, result.chunks.size)
        }

        @Test
        fun `get URL-encodes lookup table IDs with special characters`() = runTest {
            val http = createHttpClient()
            val service = LookupTablesService(http)
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
            enqueue("""{"London":{"country":"UK","population":9000000}}""")

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
            enqueue("""{"message":"pong","requestId":"req-1"}""")

            val result = client.ping()
            val recorded = server.takeRequest()

            assertEquals("GET", recorded.method)
            assertEquals("/ping", recorded.path)
            assertEquals("pong", result.message)
        }
    }

    // ========================================================================
    // Response deserialization robustness tests
    // ========================================================================

    @Nested
    inner class ResponseDeserializationContract {
        @Test
        fun `LoginResponse handles missing optional fields`() {
            val result = json.decodeFromString<LoginResponse>(
                """{"accessToken":"tok"}"""
            )
            assertEquals("tok", result.accessToken)
            assertNull(result.refreshToken)
            assertNull(result.idToken)
            assertNull(result.expiresIn)
        }

        @Test
        fun `ChatCompletionResponse handles full response shape`() {
            val result = json.decodeFromString<ChatCompletionResponse>("""{
                "id":"chatcmpl-abc",
                "object":"chat.completion",
                "created":1700000000,
                "model":"gpt-4",
                "choices":[
                    {"index":0,"message":{"role":"assistant","content":"Hi"},"finish_reason":"stop"},
                    {"index":1,"message":{"role":"assistant","content":"Hello"},"finish_reason":"stop"}
                ],
                "usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}
            }""")
            assertEquals(2, result.choices.size)
            assertEquals(30, result.usage?.totalTokens)
        }

        @Test
        fun `TemplateListResponse handles items field alias`() {
            val result = json.decodeFromString<TemplateListResponse>(
                """{"items":[{"name":"Template via items"}],"count":1}"""
            )
            assertEquals(1, result.allTemplates.size)
            assertEquals("Template via items", result.allTemplates[0].name)
        }

        @Test
        fun `TemplateListResponse handles templates field`() {
            val result = json.decodeFromString<TemplateListResponse>(
                """{"templates":[{"name":"Template via templates"}],"count":1}"""
            )
            assertEquals(1, result.allTemplates.size)
        }

        @Test
        fun `DeviceCatalogResponse handles both field names`() {
            val itemsResult = json.decodeFromString<DeviceCatalogResponse>(
                """{"items":[{"device_name":"Dev1"}],"count":1}"""
            )
            assertEquals(1, itemsResult.allDevices.size)

            val devicesResult = json.decodeFromString<DeviceCatalogResponse>(
                """{"devices":[{"device_name":"Dev2"}],"count":1}"""
            )
            assertEquals(1, devicesResult.allDevices.size)
        }

        @Test
        fun `ConsumedEvent handles empty event`() {
            val result = json.decodeFromString<ConsumedEvent>(
                """{"slug":"s1","empty":true}"""
            )
            assertEquals(true, result.empty)
            assertNull(result.text)
        }

        @Test
        fun `LookupTableDetail handles full manifest response`() {
            val result = json.decodeFromString<LookupTableDetail>("""{
                "lookup_table_id":"lt1",
                "name":"Test",
                "schema_keys":["a","b"],
                "schema_key_count":2,
                "schema_keys_truncated":false,
                "version":3,
                "payload_hash":"hash",
                "storage_mode":"chunked",
                "chunk_count":2,
                "updated_at":1700000000,
                "prompt":"Find by name",
                "default_success_sentence":"Found {{name}}",
                "default_fail_sentence":"Not found",
                "chunk_encoding":"json",
                "manifest_hash":"mhash",
                "chunks":[
                    {"index":0,"path":"/c/0","sha256":"h0","byte_length":100},
                    {"index":1,"path":"/c/1","sha256":"h1","byte_length":200}
                ]
            }""")
            assertEquals(3, result.version)
            assertEquals(2, result.chunks.size)
            assertEquals("Found {{name}}", result.defaultSuccessSentence)
        }

        @Test
        fun `EmbeddingResponse deserializes correctly`() {
            val result = json.decodeFromString<EmbeddingResponse>("""{
                "object":"list",
                "data":[{"object":"embedding","embedding":[0.1,0.2,0.3],"index":0}],
                "model":"text-embedding-ada-002",
                "usage":{"prompt_tokens":5,"total_tokens":5}
            }""")
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
        fun `CreateEndpointResponse deserializes correctly`() {
            val result = json.decodeFromString<CreateEndpointResponse>("""{
                "slug":"abc",
                "status":"active",
                "expires_at":1700000000,
                "endpoint_path":"/events/abc"
            }""")
            assertEquals("abc", result.slug)
            assertNull(result.hmacSecret)
            assertNull(result.hmacRequired)
        }

        @Test
        fun `RevokeAndReplaceResponse deserializes correctly`() {
            val result = json.decodeFromString<RevokeAndReplaceResponse>("""{
                "old_slug":"old",
                "new_slug":"new",
                "new_endpoint_path":"/events/new",
                "revoked_expires_at":1700000000,
                "new_expires_at":1700100000,
                "hmac_secret":"sec",
                "hmac_required":true
            }""")
            assertEquals("old", result.oldSlug)
            assertEquals("sec", result.hmacSecret)
        }

        @Test
        fun `RegistryAppsResponse handles apps field`() {
            val result = json.decodeFromString<RegistryAppsResponse>("""{
                "apps":[{"app_id":"a1","name":"App","slug":"app"}]
            }""")
            assertEquals(1, result.allApps.size)
            assertEquals("app", result.allApps[0].slug)
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
        fun `auth login sends email and password in body`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"accessToken":"tok"}""")

            service.login("test@example.com", "mypassword")
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""email":"test@example.com""""))
            assertTrue(body.contains(""""password":"mypassword""""))
        }

        @Test
        fun `auth register includes name when provided`() = runTest {
            val http = createHttpClient()
            val service = AuthService(http)
            enqueue("""{"userId":"u1","email":"test@example.com","confirmed":false}""")

            service.register("test@example.com", "pass", "John Doe")
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""name":"John Doe""""))
        }

        @Test
        fun `template create sends name and description`() = runTest {
            val http = createHttpClient()
            val service = TemplatesService(http)
            enqueue("""{"template_id":"t1","name":"My Template"}""")

            service.create("My Template", description = "A description")
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""name":"My Template""""))
            assertTrue(body.contains(""""description":"A description""""))
        }

        @Test
        fun `endpoint revoke sends slug in body`() = runTest {
            val http = createHttpClient()
            val service = EndpointsService(http)
            enqueue("""{"slug":"s1","revoked":true}""")

            service.revoke("s1")
            val recorded = server.takeRequest()

            val body = recorded.body.readUtf8()
            assertTrue(body.contains(""""slug":"s1""""))
        }

        @Test
        fun `AI chat completion sends correct request structure`() = runTest {
            val http = createHttpClient()
            val service = AiService(http)
            enqueue("""{"choices":[{"index":0,"message":{"role":"assistant","content":"ok"}}]}""")

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
            val service = TemplatesService(http)
            enqueue("""{"template_id":"t1","name":"Test"}""")

            service.create("Test")
            val recorded = server.takeRequest()

            val authHeader = recorded.getHeader("Authorization")
            assertNotNull(authHeader)
            assertTrue(authHeader!!.startsWith("Bearer "))
        }

        @Test
        fun `NONE endpoints do not include Authorization header`() = runTest {
            val http = createHttpClient(accessToken = null, ownerToken = null)
            val service = TemplatesService(http)
            enqueue("""{"templates":[],"count":0}""")

            service.list()
            val recorded = server.takeRequest()

            assertNull(recorded.getHeader("Authorization"))
        }

        @Test
        fun `OWNER endpoints include owner token`() = runTest {
            val http = createHttpClient(ownerToken = "my-owner-token")
            val service = EndpointsService(http)
            enqueue("""{"slug":"s1","status":"active","expires_at":1700000000,"endpoint_path":"/events/s1"}""")

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
            enqueue("""{"devices":[]}""")

            service.list()
            val recorded = server.takeRequest()

            assertEquals(TEST_APP_ID, recorded.getHeader("X-App-Id"))
        }
    }
}
