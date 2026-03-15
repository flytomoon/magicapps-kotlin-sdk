/**
 * Auto-generated API types from OpenAPI specification.
 * DO NOT EDIT MANUALLY - regenerate with: npm run openapi:generate-types
 */
package com.magicapps.sdk

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Platform health check response with per-service status */
@Serializable
data class PlatformHealthResponse(
    /** "Overall platform status: healthy (all pass), degraded (some non-critical fail), unhealthy (critical failures)" */
    val status: String,
    /** ISO 8601 timestamp of when the check was performed */
    val timestamp: String,
    /** Deployment environment identifier (dev, staging, prod) */
    val environment: String,
    /** Per-service health check results */
    val checks: Map<String, @Contextual Any>,
    val required: String? = null,
    /** Generic status message without secrets or internal details */
    val properties: String? = null
)

@Serializable
data class AuthTokenResponse(
    val user: Map<String, @Contextual Any>? = null,
    val id: String? = null,
    val email: String? = null,
    val status: String? = null,
    val token: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null
)

@Serializable
data class Tenant(
    @SerialName("tenant_id")
    val tenantId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val status: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val plan: String? = null
)

@Serializable
data class AIProvider(
    val id: String? = null,
    val name: String? = null,
    val provider: String? = null,
    val model: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Error(
    val error: String,
    val message: String
)

@Serializable
data class LookupTableSummary(
    @SerialName("lookup_table_id")
    val lookupTableId: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("schema_keys")
    val schemaKeys: List<String>? = null,
    @SerialName("schema_key_count")
    val schemaKeyCount: Int? = null,
    @SerialName("schema_keys_truncated")
    val schemaKeysTruncated: Boolean? = null,
    val version: Int? = null,
    @SerialName("payload_hash")
    val payloadHash: String? = null,
    @SerialName("storage_mode")
    val storageMode: String? = null,
    @SerialName("chunk_count")
    val chunkCount: Int? = null,
    @SerialName("updated_at")
    val updatedAt: Int? = null
)

@Serializable
data class LookupTableChunk(
    val index: Int? = null,
    val path: String? = null,
    val sha256: String? = null,
    @SerialName("byte_length")
    val byteLength: Int? = null
)

@Serializable
data class LookupTableDetail(
    /** Present on detail only; omitted from summary list. */
    val prompt: String? = null,
    /** Optional templated success sentence using {{path.to.key}} tokens. */
    @SerialName("default_success_sentence")
    val defaultSuccessSentence: String? = null,
    /** Optional fallback fail sentence. */
    @SerialName("default_fail_sentence")
    val defaultFailSentence: String? = null,
    @SerialName("chunk_encoding")
    val chunkEncoding: String? = null,
    @SerialName("manifest_hash")
    val manifestHash: String? = null,
    val chunks: List<LookupTableChunk>? = null
) // extends: LookupTableSummary, LookupTableChunk

@Serializable
data class AdminLookupTableDetail(
    @SerialName("allowlisted_apps")
    val allowlistedApps: List<String>? = null,
    @SerialName("client_targets")
    val clientTargets: List<String>? = null,
    val status: String? = null,
    @SerialName("created_at")
    val createdAt: Int? = null,
    @SerialName("updated_by")
    val updatedBy: String? = null,
    @SerialName("deleted_at")
    val deletedAt: Int? = null,
    @SerialName("purge_at")
    val purgeAt: Int? = null,
    @SerialName("payload_json")
    val payloadJson: Map<String, @Contextual Any>? = null,
    @SerialName("manifest_key")
    val manifestKey: String? = null
) // extends: LookupTableDetail

@Serializable
data class AdminLookupTableUpsertRequest(
    @SerialName("lookup_table_id")
    val lookupTableId: String? = null,
    val name: String,
    val description: String? = null,
    /** Optional prompt metadata (max 4000 chars). */
    val prompt: String? = null,
    /** Optional success sentence template (max 2000 chars). */
    @SerialName("default_success_sentence")
    val defaultSuccessSentence: String? = null,
    /** Optional fail sentence text (max 1000 chars). */
    @SerialName("default_fail_sentence")
    val defaultFailSentence: String? = null,
    @SerialName("allowlisted_apps")
    val allowlistedApps: List<String>? = null,
    @SerialName("client_targets")
    val clientTargets: List<String>? = null,
    /** Required on PATCH for optimistic locking. */
    val version: Int? = null,
    @SerialName("payload_json")
    val payloadJson: Map<String, @Contextual Any>
)

@Serializable
data class Template(
    val pk: String? = null,
    val sk: String? = null,
    @SerialName("template_id")
    val templateId: String? = null,
    @SerialName("integration_id")
    val integrationId: String? = null,
    @SerialName("app_id")
    val appId: String? = null,
    @SerialName("template_name")
    val templateName: String? = null,
    @SerialName("template_type")
    val templateType: String? = null,
    /** > */
    @SerialName("source_mode")
    val sourceMode: String? = null,
    /** Configuration for api_poll source_mode. Only applies when source_mode=api_poll. */
    @SerialName("poll_config")
    val pollConfig: Map<String, @Contextual Any>? = null,
    /** > */
    @SerialName("poll_mode")
    val pollMode: String? = null,
    /** Maximum time in milliseconds to wait for a result (1ms–300000ms / 5 min). */
    @SerialName("timeout_ms")
    val timeoutMs: Int? = null,
    /** Maximum number of poll attempts before giving up (1–100). */
    @SerialName("max_attempts")
    val maxAttempts: Int? = null,
    /** Delay in milliseconds between poll attempts (0–60000ms). */
    @SerialName("backoff_ms")
    val backoffMs: Int? = null,
    /** > */
    @SerialName("empty_result_behavior")
    val emptyResultBehavior: String? = null,
    /** API-poll-specific response parsing configuration. Only applies when source_mode=api_poll. */
    @SerialName("api_poll_config")
    val apiPollConfig: Map<String, @Contextual Any>? = null,
    /** > */
    @SerialName("response_type")
    val responseType: String? = null,
    /** > */
    @SerialName("response_path")
    val responsePath: String? = null,
    /** High-level grouping (e.g., custom_core, built_integration) */
    val group: String? = null,
    /** End-user facing description shown publicly */
    @SerialName("public_description")
    val publicDescription: String? = null,
    /** How the endpoint is supplied (e.g., full_url, id_only) */
    @SerialName("endpoint_input_mode")
    val endpointInputMode: String? = null,
    /** Placeholder text for id_only inputs */
    @SerialName("endpoint_input_placeholder")
    val endpointInputPlaceholder: String? = null,
    /** Whether the client should show the endpoint input field */
    @SerialName("show_endpoint_input")
    val showEndpointInput: Boolean? = null,
    /** Whether the client should display parameter fields */
    @SerialName("show_parameters")
    val showParameters: Boolean? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    val provider: String? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val status: String? = null,
    val version: String? = null,
    @SerialName("is_latest")
    val isLatest: Boolean? = null,
    @SerialName("last_verified_at")
    val lastVerifiedAt: String? = null,
    val maintainer: String? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("price_tier")
    val priceTier: String? = null,
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("auth_type")
    val authType: String? = null,
    @SerialName("auth_location")
    val authLocation: String? = null,
    val scopes: List<String>? = null,
    @SerialName("requires_signature")
    val requiresSignature: Boolean? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("submitted_by_name")
    val submittedByName: String? = null,
    @SerialName("submitted_by_email")
    val submittedByEmail: String? = null,
    @SerialName("submitted_at")
    val submittedAt: String? = null,
    @SerialName("breaking_changes")
    val breakingChanges: String? = null,
    @SerialName("supersedes_version")
    val supersedesVersion: String? = null,
    @SerialName("approved_at")
    val approvedAt: String? = null,
    @SerialName("is_new_until")
    val isNewUntil: String? = null,
    val visibility: TemplateVisibility? = null,
    @SerialName("allowed_app_ids")
    val allowedAppIds: List<String>? = null,
    @SerialName("endpoint_pattern")
    val endpointPattern: String? = null,
    val parameters: List<TemplateParameter>? = null,
    val metadata: Map<String, @Contextual Any>? = null,
    @SerialName("created_at")
    val createdAt: Double? = null,
    @SerialName("updated_at")
    val updatedAt: Double? = null
)

@Serializable
data class AppIntegration(
    @SerialName("integration_id")
    val integrationId: String? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    val group: String? = null,
    @SerialName("template_id")
    val templateId: String? = null,
    @SerialName("template_name")
    val templateName: String? = null,
    @SerialName("template_type")
    val templateType: String? = null,
    @SerialName("endpoint_input_mode")
    val endpointInputMode: String? = null,
    @SerialName("endpoint_input_placeholder")
    val endpointInputPlaceholder: String? = null,
    @SerialName("show_endpoint_input")
    val showEndpointInput: Boolean? = null,
    @SerialName("show_parameters")
    val showParameters: Boolean? = null,
    @SerialName("endpoint_pattern")
    val endpointPattern: String? = null,
    val parameters: List<TemplateParameter>? = null,
    val metadata: Map<String, @Contextual Any>? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("auth_type")
    val authType: String? = null,
    @SerialName("auth_location")
    val authLocation: String? = null,
    val scopes: List<String>? = null,
    @SerialName("requires_signature")
    val requiresSignature: Boolean? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("setup_fields")
    val setupFields: List<SetupField>? = null
)

@Serializable
data class AppIntegrationV2(
    @SerialName("integration_id")
    val integrationId: String? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    val group: String? = null,
    @SerialName("template_id")
    val templateId: String? = null,
    @SerialName("template_name")
    val templateName: String? = null,
    @SerialName("template_type")
    val templateType: String? = null,
    @SerialName("endpoint_input_mode")
    val endpointInputMode: String? = null,
    @SerialName("endpoint_input_placeholder")
    val endpointInputPlaceholder: String? = null,
    @SerialName("show_endpoint_input")
    val showEndpointInput: Boolean? = null,
    @SerialName("show_parameters")
    val showParameters: Boolean? = null,
    @SerialName("endpoint_pattern")
    val endpointPattern: String? = null,
    val parameters: List<TemplateParameter>? = null,
    val metadata: Map<String, @Contextual Any>? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("auth_type")
    val authType: String? = null,
    @SerialName("auth_location")
    val authLocation: String? = null,
    val scopes: List<String>? = null,
    @SerialName("requires_signature")
    val requiresSignature: Boolean? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("setup_fields")
    val setupFields: List<SetupField>? = null
)

@Serializable
data class App(
    @SerialName("app_id")
    val appId: String? = null,
    val name: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val summary: String? = null,
    @SerialName("allow_multiple")
    val allowMultiple: Boolean? = null,
    @SerialName("public_description")
    val publicDescription: String? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val aliases: List<String>? = null,
    @SerialName("default_integration_id")
    val defaultIntegrationId: String? = null,
    val status: String? = null,
    val version: String? = null,
    @SerialName("is_latest")
    val isLatest: Boolean? = null,
    @SerialName("last_verified_at")
    val lastVerifiedAt: String? = null,
    val maintainer: String? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("created_by_email")
    val createdByEmail: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    val visibility: TemplateVisibility? = null,
    val integrations: List<AppIntegration>? = null
)

@Serializable
data class AppV2(
    @SerialName("app_id")
    val appId: String? = null,
    val name: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val summary: String? = null,
    @SerialName("allow_multiple")
    val allowMultiple: Boolean? = null,
    @SerialName("public_description")
    val publicDescription: String? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val aliases: List<String>? = null,
    @SerialName("default_integration_id")
    val defaultIntegrationId: String? = null,
    val status: String? = null,
    val version: String? = null,
    @SerialName("is_latest")
    val isLatest: Boolean? = null,
    @SerialName("last_verified_at")
    val lastVerifiedAt: String? = null,
    val maintainer: String? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("created_by_email")
    val createdByEmail: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    val visibility: TemplateVisibility? = null,
    val integrations: List<AppIntegrationV2>? = null
)

@Serializable
data class AppAvailabilityIntegration(
    @SerialName("integration_id")
    val integrationId: String? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    @SerialName("template_type")
    val templateType: String? = null
)

@Serializable
data class AppAvailabilityMatch(
    @SerialName("app_id")
    val appId: String? = null,
    val name: String? = null,
    @SerialName("default_integration_id")
    val defaultIntegrationId: String? = null,
    val integrations: List<AppAvailabilityIntegration>? = null
)

@Serializable
data class AppAvailabilityResponse(
    val available: Boolean? = null,
    val matches: List<AppAvailabilityMatch>? = null
)

@Serializable
data class TemplateInput(
    @SerialName("template_type")
    val templateType: String? = null,
    @SerialName("public_description")
    val publicDescription: String? = null,
    /** How the endpoint is supplied (e.g., full_url, id_only) */
    @SerialName("endpoint_input_mode")
    val endpointInputMode: String? = null,
    /** Placeholder text for id_only inputs */
    @SerialName("endpoint_input_placeholder")
    val endpointInputPlaceholder: String? = null,
    /** Whether the client should show the endpoint input field */
    @SerialName("show_endpoint_input")
    val showEndpointInput: Boolean? = null,
    /** Whether the client should display parameter fields */
    @SerialName("show_parameters")
    val showParameters: Boolean? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val status: String? = null,
    val version: String? = null,
    @SerialName("is_latest")
    val isLatest: Boolean? = null,
    val description: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("price_tier")
    val priceTier: String? = null,
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("submitted_by_name")
    val submittedByName: String? = null,
    @SerialName("submitted_by_email")
    val submittedByEmail: String? = null,
    @SerialName("submitted_at")
    val submittedAt: String? = null,
    val visibility: TemplateVisibility? = null,
    val parameters: List<TemplateParameter>? = null,
    val metadata: Map<String, @Contextual Any>? = null
)

/** Mutable fields admins can update on approved templates; approved_at/is_new_until remain unchanged. */
@Serializable
data class TemplateAdminUpdate(
    @SerialName("template_name")
    val templateName: String? = null,
    @SerialName("public_description")
    val publicDescription: String? = null,
    val description: String? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    val provider: String? = null,
    @SerialName("endpoint_pattern")
    val endpointPattern: String? = null,
    @SerialName("endpoint_input_mode")
    val endpointInputMode: String? = null,
    @SerialName("endpoint_input_placeholder")
    val endpointInputPlaceholder: String? = null,
    @SerialName("show_endpoint_input")
    val showEndpointInput: Boolean? = null,
    @SerialName("show_parameters")
    val showParameters: Boolean? = null,
    val parameters: List<TemplateParameter>? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val maintainer: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("price_tier")
    val priceTier: String? = null,
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("breaking_changes")
    val breakingChanges: String? = null,
    @SerialName("supersedes_version")
    val supersedesVersion: String? = null,
    val visibility: TemplateVisibility? = null
)

@Serializable
data class TemplateParameter(
    val name: String,
    /** Legacy alias for value_type. */
    val type: String? = null,
    /** Preferred field for parameter value type. */
    @SerialName("value_type")
    val valueType: String? = null,
    /** User-facing label when value_type is user_input. */
    val label: String? = null,
    val required: Boolean? = null,
    @SerialName("default")
    val defaultValue: String? = null,
    val example: String? = null,
    val encoding: String? = null
)

@Serializable
data class SetupField(
    val id: String? = null,
    val label: String? = null,
    val type: String? = null,
    val required: Boolean? = null,
    val placeholder: String? = null,
    val hint: String? = null,
    @SerialName("input_mode")
    val inputMode: String? = null,
    @SerialName("expected_format")
    val expectedFormat: String? = null,
    val validation: Map<String, @Contextual Any>? = null,
    @SerialName("is_secret")
    val isSecret: Boolean? = null,
    @SerialName("allow_voice_input")
    val allowVoiceInput: Boolean? = null
)

@Serializable
data class TemplateVisibility(
    val registry: Boolean? = null,
    val templates: Boolean? = null,
    val wellKnown: Boolean? = null
)

@Serializable
data class Device(
    val id: String? = null,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("device_type")
    val deviceType: String? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val visibility: String? = null,
    @SerialName("bluetooth_uuid")
    val bluetoothUuid: String? = null,
    val status: String? = null,
    val version: String? = null,
    @SerialName("is_latest")
    val isLatest: Boolean? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    @SerialName("allowed_app_ids")
    val allowedAppIds: List<String>? = null,
    val metadata: Map<String, @Contextual Any>? = null,
    @SerialName("created_at")
    val createdAt: Double? = null,
    @SerialName("updated_at")
    val updatedAt: Double? = null
)

@Serializable
data class DeviceInput(
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("device_type")
    val deviceType: String,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val visibility: String,
    @SerialName("bluetooth_uuid")
    val bluetoothUuid: String? = null,
    val status: String? = null,
    val version: String? = null,
    @SerialName("is_latest")
    val isLatest: Boolean? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    val metadata: Map<String, @Contextual Any>? = null
)

@Serializable
data class SubmissionReviewInput(
    val status: String,
    @SerialName("review_notes")
    val reviewNotes: String? = null,
    /** ISO8601 timestamp; optional override (defaults to approval +14 days) */
    @SerialName("is_new_until")
    val isNewUntil: String? = null,
    @SerialName("reviewed_by")
    val reviewedBy: String? = null,
    @SerialName("reviewed_at")
    val reviewedAt: String? = null
)

/** | */
@Serializable
data class SubmissionAdminUpdate(
    val status: String? = null,
    @SerialName("review_notes")
    val reviewNotes: String? = null,
    @SerialName("is_new_until")
    val isNewUntil: String? = null,
    val action: String? = null,
    val message: String? = null,
    @SerialName("template_name")
    val templateName: String? = null,
    @SerialName("template_type")
    val templateType: String? = null,
    @SerialName("integration_name")
    val integrationName: String? = null,
    val provider: String? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("public_description")
    val publicDescription: String? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val maintainer: String? = null,
    val visibility: String? = null,
    @SerialName("endpoint_pattern")
    val endpointPattern: String? = null,
    @SerialName("endpoint_input_mode")
    val endpointInputMode: String? = null,
    @SerialName("endpoint_input_placeholder")
    val endpointInputPlaceholder: String? = null,
    @SerialName("show_endpoint_input")
    val showEndpointInput: Boolean? = null,
    @SerialName("show_parameters")
    val showParameters: Boolean? = null,
    val parameters: List<TemplateParameter>? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("docs_url")
    val docsUrl: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("app_store_urls")
    val appStoreUrls: Map<String, @Contextual Any>? = null,
    val apple: String? = null,
    val google: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("price_tier")
    val priceTier: String? = null,
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("breaking_changes")
    val breakingChanges: String? = null,
    @SerialName("supersedes_version")
    val supersedesVersion: String? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("auth_type")
    val authType: String? = null,
    @SerialName("auth_location")
    val authLocation: String? = null,
    @SerialName("requires_signature")
    val requiresSignature: Boolean? = null,
    @SerialName("perform_app_install_check")
    val performAppInstallCheck: Boolean? = null,
    @SerialName("url_scheme_param_mode")
    val urlSchemeParamMode: String? = null,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("device_type")
    val deviceType: String? = null,
    @SerialName("allowed_app_ids")
    val allowedAppIds: List<String>? = null,
    val metadata: Map<String, @Contextual Any>? = null
)

@Serializable
data class Submission(
    val id: String? = null,
    /** app/template/device */
    val type: String? = null,
    val status: String? = null,
    @SerialName("review_notes")
    val reviewNotes: String? = null,
    @SerialName("reviewed_by")
    val reviewedBy: String? = null,
    @SerialName("reviewed_at")
    val reviewedAt: String? = null,
    @SerialName("is_new_until")
    val isNewUntil: String? = null,
    @SerialName("approved_at")
    val approvedAt: String? = null,
    @SerialName("status_type")
    val statusType: String? = null,
    @SerialName("generated_app_id")
    val generatedAppId: String? = null,
    @SerialName("reviewed_by_name")
    val reviewedByName: String? = null,
    @SerialName("last_submitter_email_status")
    val lastSubmitterEmailStatus: String? = null,
    @SerialName("last_submitter_email_at")
    val lastSubmitterEmailAt: String? = null,
    @SerialName("last_submitter_email_error")
    val lastSubmitterEmailError: String? = null,
    @SerialName("last_admin_email_status")
    val lastAdminEmailStatus: String? = null,
    @SerialName("last_admin_email_at")
    val lastAdminEmailAt: String? = null,
    @SerialName("last_admin_email_error")
    val lastAdminEmailError: String? = null,
    val thread: List<SubmissionThreadEntry>? = null,
    @SerialName("submitted_at")
    val submittedAt: String? = null,
    @SerialName("submitted_by_email")
    val submittedByEmail: String? = null,
    @SerialName("submitted_by_name")
    val submittedByName: String? = null
)

@Serializable
data class SubmissionReview(
    val status: String,
    @SerialName("review_notes")
    val reviewNotes: String? = null,
    @SerialName("reviewed_by")
    val reviewedBy: String? = null,
    @SerialName("reviewed_at")
    val reviewedAt: String? = null,
    @SerialName("is_new_until")
    val isNewUntil: String? = null
)

@Serializable
data class SubmissionThreadEntry(
    val author: String? = null,
    @SerialName("author_name")
    val authorName: String? = null,
    val role: String? = null,
    val type: String? = null,
    val status: String? = null,
    val message: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class Registry(
    val version: String? = null,
    val templates: List<Template>? = null,
    val apps: List<App>? = null
)
