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
        // Source: openapi.yaml Template schema — template_name, created_at/updated_at are type: number (epoch)
        val jsonStr = """
        {
            "template_id": "tmpl-1",
            "app_id": "test-app",
            "template_name": "Test Template",
            "description": null,
            "created_at": 1735689600,
            "updated_at": 1735689600
        }
        """.trimIndent()

        val template = json.decodeFromString<Template>(jsonStr)
        assertEquals("tmpl-1", template.templateId)
        assertEquals("test-app", template.appId)
        assertEquals("Test Template", template.templateName)
        assertNull(template.description)
    }

    @Test
    fun `ApiException includes status code`() {
        val exception = ApiException("Not Found", 404)
        assertEquals(404, exception.status)
        assert(exception.message!!.contains("Not Found"))
    }

}
