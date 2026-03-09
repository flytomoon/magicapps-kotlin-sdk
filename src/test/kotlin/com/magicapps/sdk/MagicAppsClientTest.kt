package com.magicapps.sdk

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MagicAppsClientTest {

    @Test
    fun `config requires non-blank baseUrl`() {
        assertThrows<IllegalArgumentException> {
            MagicAppsConfig(baseUrl = "", appId = "test")
        }
    }

    @Test
    fun `config requires non-blank appId`() {
        assertThrows<IllegalArgumentException> {
            MagicAppsConfig(baseUrl = "https://api.example.com", appId = "")
        }
    }

    @Test
    fun `config defaults timeout to 30 seconds`() {
        val config = MagicAppsConfig(
            baseUrl = "https://api.example.com",
            appId = "test-app"
        )
        assertEquals(30_000L, config.timeout)
    }

    @Test
    fun `config accepts custom auth token and timeout`() {
        val config = MagicAppsConfig(
            baseUrl = "https://api.example.com",
            appId = "test-app",
            authToken = "my-token",
            timeout = 60_000L
        )
        assertEquals("my-token", config.authToken)
        assertEquals(60_000L, config.timeout)
    }

    @Test
    fun `client instantiates with valid config`() {
        val config = MagicAppsConfig(
            baseUrl = "https://api.example.com",
            appId = "test-app"
        )
        val client = MagicAppsClient(config)
        assertNotNull(client)
    }

    @Test
    fun `AppInfo deserializes from JSON`() {
        val json = """
        {
            "app_id": "test-app",
            "name": "Test App",
            "slug": "test-app",
            "description": "A test application",
            "created_at": "2025-01-01T00:00:00Z",
            "updated_at": "2025-01-01T00:00:00Z"
        }
        """.trimIndent()

        val appInfo = Json.decodeFromString<AppInfo>(json)
        assertEquals("test-app", appInfo.appId)
        assertEquals("Test App", appInfo.name)
        assertEquals("test-app", appInfo.slug)
        assertEquals("A test application", appInfo.description)
    }

    @Test
    fun `Template deserializes from JSON`() {
        val json = """
        {
            "template_id": "tmpl-1",
            "app_id": "test-app",
            "name": "Test Template",
            "description": null,
            "created_at": "2025-01-01T00:00:00Z",
            "updated_at": "2025-01-01T00:00:00Z"
        }
        """.trimIndent()

        val template = Json.decodeFromString<Template>(json)
        assertEquals("tmpl-1", template.templateId)
        assertEquals("test-app", template.appId)
        assertEquals("Test Template", template.name)
        assertNull(template.description)
    }

    @Test
    fun `ApiException includes status code in message`() {
        val exception = ApiException(404, "Not Found", null)
        assertEquals(404, exception.statusCode)
        assert(exception.message!!.contains("404"))
    }
}
