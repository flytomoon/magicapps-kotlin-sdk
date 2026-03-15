package com.magicapps.sdk

import com.magicapps.sdk.core.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MagicAppsClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `client instantiates with valid config`() {
        val config = SdkConfig(
            baseUrl = "https://api.example.com",
            appId = "test-app"
        )
        val client = MagicAppsClient(config)
        assertNotNull(client)
    }

    @Test
    fun `Template deserializes from JSON`() {
        val jsonStr = """
        {
            "template_id": "tmpl-1",
            "app_id": "test-app",
            "name": "Test Template",
            "description": null,
            "created_at": "2025-01-01T00:00:00Z",
            "updated_at": "2025-01-01T00:00:00Z"
        }
        """.trimIndent()

        val template = json.decodeFromString<Template>(jsonStr)
        assertEquals("tmpl-1", template.templateId)
        assertEquals("test-app", template.appId)
        assertEquals("Test Template", template.name)
        assertNull(template.description)
    }

    @Test
    fun `ApiException includes status code`() {
        val exception = ApiException("Not Found", 404)
        assertEquals(404, exception.status)
        assert(exception.message!!.contains("Not Found"))
    }

    @Test
    fun `AiUsageRecord deserializes from JSON`() {
        val jsonStr = """
        {
            "usage_id": "u-abc-123",
            "app_id": "test-app",
            "provider_id": "openai",
            "model_id": "gpt-4o-mini",
            "request_type": "chat",
            "input_tokens": 100,
            "output_tokens": 50,
            "total_tokens": 150,
            "latency_ms": 420,
            "status": "success",
            "created_at": 1709251200000.0,
            "expires_at": 1717027200.0,
            "user_id": "user-42"
        }
        """.trimIndent()

        val record = json.decodeFromString<com.magicapps.sdk.services.AiUsageRecord>(jsonStr)
        assertEquals("u-abc-123", record.usageId)
        assertEquals("test-app", record.appId)
        assertEquals("openai", record.providerId)
        assertEquals("gpt-4o-mini", record.modelId)
        assertEquals("chat", record.requestType)
        assertEquals(100, record.inputTokens)
        assertEquals(50, record.outputTokens)
        assertEquals(150, record.totalTokens)
        assertEquals(420, record.latencyMs)
        assertEquals("success", record.status)
        assertNull(record.errorCode)
        assertEquals("user-42", record.userId)
    }

    @Test
    fun `AiUsageRecord deserializes with error fields`() {
        val jsonStr = """
        {
            "usage_id": "u-err-456",
            "app_id": "test-app",
            "provider_id": "anthropic",
            "model_id": "claude-sonnet-4-20250514",
            "request_type": "chat",
            "input_tokens": 0,
            "output_tokens": 0,
            "total_tokens": 0,
            "latency_ms": 120,
            "status": "error",
            "created_at": 1709251200000.0,
            "expires_at": 1717027200.0,
            "error_code": "rate_limited"
        }
        """.trimIndent()

        val record = json.decodeFromString<com.magicapps.sdk.services.AiUsageRecord>(jsonStr)
        assertEquals("error", record.status)
        assertEquals("rate_limited", record.errorCode)
        assertNull(record.userId)
    }

    @Test
    fun `AiUsageResponse deserializes from JSON`() {
        val jsonStr = """
        {
            "usage": [
                {
                    "usage_id": "u-1",
                    "app_id": "test-app",
                    "provider_id": "openai",
                    "model_id": "gpt-4o-mini",
                    "request_type": "chat",
                    "input_tokens": 100,
                    "output_tokens": 50,
                    "total_tokens": 150,
                    "latency_ms": 420,
                    "status": "success",
                    "created_at": 1709251200000.0,
                    "expires_at": 1717027200.0
                }
            ],
            "count": 1
        }
        """.trimIndent()

        val response = json.decodeFromString<com.magicapps.sdk.services.AiUsageResponse>(jsonStr)
        assertEquals(1, response.count)
        assertEquals(1, response.usage.size)
        assertEquals("u-1", response.usage[0].usageId)
    }
}
