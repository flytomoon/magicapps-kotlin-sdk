# Magic Apps Cloud SDK (Kotlin)

Official Kotlin/Android SDK for the Magic Apps Cloud platform.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("cloud.magicapps:magicapps-cloud-sdk:0.3.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'cloud.magicapps:magicapps-cloud-sdk:0.3.0'
}
```

### GitHub Packages

If publishing via GitHub Packages, add the repository to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/magicapps/magicapps-infra")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

## Quick Start

```kotlin
import com.magicapps.sdk.MagicAppsClient
import com.magicapps.sdk.core.SdkConfig

val client = MagicAppsClient(SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id"
))

// Health check
val pong = client.ping()
println(pong.message)

// Get a template
val template = client.templates.get("template-id")
println(template.templateName)
```

## Authentication

### Token-Based

Pass tokens directly when initializing the client:

```kotlin
val client = MagicAppsClient(SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id",
    accessToken = "your-jwt-token",
    refreshToken = "your-refresh-token"
))

// Or update tokens later
client.setTokens(accessToken = "new-token", refreshToken = "new-refresh-token")

// Clear tokens on logout
client.clearTokens()
```

### Google Sign-In (Android)

```kotlin
val result = client.googleAuth.exchangeToken(idToken = googleIdToken)
// Tokens are stored automatically
```

### Passkeys

```kotlin
// Registration flow
val options = client.auth.getPasskeyRegisterOptions()
// ... use options.challenge with Android Credential Manager ...
val result = client.auth.verifyPasskeyRegistration(credentialJson)

// Authentication flow
val authOptions = client.auth.getPasskeyAuthOptions()
// ... use authOptions.challenge with Android Credential Manager ...
val authResult = client.auth.verifyPasskeyAuth(assertionJson)
```

### Email Magic Link

```kotlin
// Request a magic link
client.auth.requestEmailMagicLink("user@example.com")

// Verify the token from the link
val result = client.auth.verifyEmailMagicLink(token)
```

## Services

### Templates

```kotlin
// Get a specific template
val template = client.templates.get("template-id")

// Get app catalog
val catalog = client.templates.getCatalog()
```

### Owner

```kotlin
// Register a device owner (returns owner token)
val result = client.owner.registerOwner(
    deviceOwnerId = "device-uuid",
    appId = "your-app-id"
)

// Migrate owner data to a signed-in user
client.owner.migrateOwnerToUser(
    deviceOwnerId = "device-uuid",
    appId = "your-app-id"
)
```

### Settings & Config

```kotlin
// Get/update user settings
val settings = client.settings.getSettings()
client.settings.updateSettings(JsonObject(mapOf("key" to JsonPrimitive("value"))))

// Get/update config
val config = client.settings.getConfig()
client.settings.updateConfig(configBody)

// Integration secrets
val secret = client.settings.getIntegrationSecret("integration-id")
client.settings.uploadIntegrationSecret("integration-id", secretBody)
```

### Devices

```kotlin
// Fetch device catalog
val catalog = client.devices.list()
for (device in catalog.allDevices) {
    println("${device.name}: ${device.manufacturer}")
}

// Convenience: get flat list
val allDevices = client.devices.getAll()
```

### Endpoints & Events

```kotlin
// Create a webhook endpoint
val endpoint = client.endpoints.create()
println("Slug: ${endpoint.slug}")
println("Path: ${endpoint.endpointPath}")

// Post an event to an endpoint
val event = client.endpoints.postEvent(
    slug = endpoint.slug,
    payload = """{"message": "Hello"}"""
)

// Post with HMAC signing
val signedEvent = client.endpoints.postEvent(
    slug = endpoint.slug,
    payload = """{"message": "Signed hello"}""",
    hmacSecret = endpoint.hmacSecret
)

// Consume an event (single-slot, consume-on-read)
val consumed = client.endpoints.consumeEvent(endpoint.slug)

// Revoke and replace an endpoint
val replacement = client.endpoints.revokeAndReplace(endpoint.slug)
println("New slug: ${replacement.newSlug}")
```

### AI Services

```kotlin
import com.magicapps.sdk.services.ChatCompletionRequest
import com.magicapps.sdk.services.ChatMessage

// Simple chat
val chatResult = client.ai.chat("What is Kotlin?")
println(chatResult.choices.first().message.content)

// Full chat completion request
val request = ChatCompletionRequest(
    messages = listOf(
        ChatMessage(role = "system", content = "You are a helpful assistant."),
        ChatMessage(role = "user", content = "Explain coroutines.")
    ),
    model = "gpt-4",
    temperature = 0.7
)
val completion = client.ai.createChatCompletion(request)

// Generate embeddings
val embeddings = client.ai.createEmbedding("Hello world")

// Generate images
val images = client.ai.createImage(
    prompt = "A sunset over mountains",
    n = 1,
    size = "1024x1024"
)

// Content moderation
val moderation = client.ai.createModeration("Some user content")
if (moderation.results.first().flagged) {
    println("Content flagged!")
}

```

### Lookup Tables

```kotlin
// List available lookup tables
val tables = client.lookupTables.list()

// Get table metadata
val detail = client.lookupTables.get("table-id")

// Fetch a specific chunk
val chunk = client.lookupTables.getChunk("table-id", chunkIndex = 0)

// Fetch the complete dataset (all chunks merged)
val fullData = client.lookupTables.getFullDataset("table-id")
```

## Error Handling

The SDK uses a typed exception hierarchy for clear error handling:

```kotlin
import com.magicapps.sdk.core.*

try {
    val template = client.templates.get("template-id")
} catch (e: UnauthorizedException) {
    // 401 - Token expired or invalid
    println("Auth error: ${e.message}")
} catch (e: ForbiddenException) {
    // 403 - Insufficient permissions
    println("Forbidden: ${e.message}")
} catch (e: NotFoundException) {
    // 404 - Resource not found
    println("Not found: ${e.message}")
} catch (e: RateLimitException) {
    // 429 - Too many requests
    println("Rate limited: ${e.message}")
} catch (e: ServerException) {
    // 5xx - Server error
    println("Server error (${e.status}): ${e.message}")
} catch (e: NetworkException) {
    // Network connectivity issues
    println("Network error: ${e.message}")
} catch (e: ApiException) {
    // Any other API error
    println("API error ${e.status}: ${e.message}")
    println("Error type: ${e.errorType}")
    println("Request ID: ${e.payload?.request_id}")
}
```

## Configuration

### Full Configuration Options

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `baseUrl` | `String` | Yes | - | Base URL of the Magic Apps Cloud API |
| `appId` | `String` | Yes | - | Your registered application ID |
| `accessToken` | `String?` | No | `null` | JWT token for user authentication |
| `refreshToken` | `String?` | No | `null` | Refresh token for automatic renewal |
| `ownerToken` | `String?` | No | `null` | Owner token for owner-level auth |
| `retries` | `Int` | No | `2` | Number of retries for failed requests |
| `retryDelayMs` | `Long` | No | `250` | Base delay between retries in ms |
| `onTokenRefresh` | `((TokenPair) -> Unit)?` | No | `null` | Callback when tokens are refreshed |
| `onAuthError` | `((SdkException) -> Unit)?` | No | `null` | Callback when token refresh fails |
| `certificatePinning` | `CertificatePinningConfig?` | No | `null` | Certificate pinning configuration |
| `tokenStorage` | `TokenStorage` | No | `EncryptedFileTokenStorage()` | Token persistence backend |

### Certificate Pinning

```kotlin
val config = SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id",
    certificatePinning = CertificatePinningConfig(
        pins = listOf("sha256/YourPublicKeyHashHere="),
        includeBuiltInPins = true,
        enabled = true
    )
)
```

Disable pinning for development:

```kotlin
certificatePinning = CertificatePinningConfig(enabled = false)
```

### Token Storage

The SDK defaults to `EncryptedFileTokenStorage` for secure persistence. You can provide alternative implementations:

```kotlin
// In-memory only (tokens lost when app closes)
val config = SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id",
    tokenStorage = InMemoryTokenStorage()
)

// Custom storage (e.g., Android EncryptedSharedPreferences)
class SharedPrefsTokenStorage(context: Context) : TokenStorage {
    private val prefs = EncryptedSharedPreferences.create(
        "magicapps_tokens", /* ... */
    )
    override fun save(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun load(key: String): String? = prefs.getString(key, null)
    override fun delete(key: String) { prefs.edit().remove(key).apply() }
    override fun deleteAll() { prefs.edit().clear().apply() }
}

val config = SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id",
    tokenStorage = SharedPrefsTokenStorage(context)
)
```

### Token Refresh Callbacks

```kotlin
val config = SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id",
    onTokenRefresh = { tokenPair ->
        println("Tokens refreshed: ${tokenPair.accessToken}")
    },
    onAuthError = { error ->
        println("Auth failed: ${error.message}")
        // Navigate to login screen
    }
)
```

### Retry Configuration

```kotlin
val config = SdkConfig(
    baseUrl = "https://api.yourplatform.com",
    appId = "your-app-id",
    retries = 3,          // Retry up to 3 times
    retryDelayMs = 500    // 500ms base delay between retries
)
```

## HMAC Signature Helpers

The SDK provides standalone functions for generating and verifying HMAC signatures on endpoint events:

```kotlin
import com.magicapps.sdk.services.generateHmacSignature
import com.magicapps.sdk.services.verifyHmacSignature

// Generate a signature for posting
val headers = generateHmacSignature(
    slug = "my-endpoint-slug",
    body = """{"data": "payload"}""",
    secret = "hmac-secret-from-endpoint"
)
// Use headers.signature and headers.timestamp in your request

// Verify an incoming webhook signature
val isValid = verifyHmacSignature(
    slug = "my-endpoint-slug",
    body = rawBody,
    signature = request.getHeader("X-Signature"),
    timestamp = request.getHeader("X-Timestamp"),
    secret = "hmac-secret-from-endpoint",
    maxSkewSeconds = 300
)
```

## Requirements

- Kotlin 1.9+
- JDK 17+
- Ktor 2.3+ (HTTP client)
- kotlinx.serialization 1.6+ (JSON parsing)

## License

MIT
