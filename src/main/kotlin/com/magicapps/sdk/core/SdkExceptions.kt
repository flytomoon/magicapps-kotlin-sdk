package com.magicapps.sdk.core

import kotlinx.serialization.Serializable

/**
 * Structured error payload from the MagicApps API.
 */
@Serializable
data class ApiErrorPayload(
    val statusCode: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val code: String? = null,
    val request_id: String? = null
)

/**
 * Base exception for all SDK errors.
 */
open class SdkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Error returned when the API responds with an error status code.
 */
open class ApiException(
    message: String,
    val status: Int,
    val errorType: String? = null,
    val payload: ApiErrorPayload? = null
) : SdkException(message)

/** 401 Unauthorized. */
class UnauthorizedException(message: String, payload: ApiErrorPayload? = null) :
    ApiException(message, 401, payload?.error, payload)

/** 403 Forbidden. */
class ForbiddenException(message: String, payload: ApiErrorPayload? = null) :
    ApiException(message, 403, payload?.error, payload)

/** 404 Not Found. */
class NotFoundException(message: String, payload: ApiErrorPayload? = null) :
    ApiException(message, 404, payload?.error, payload)

/** 429 Too Many Requests. */
class RateLimitException(message: String, payload: ApiErrorPayload? = null) :
    ApiException(message, 429, payload?.error, payload)

/** 5xx Server Error. */
class ServerException(message: String, status: Int, payload: ApiErrorPayload? = null) :
    ApiException(message, status, payload?.error, payload)

/** Network error. */
class NetworkException(message: String, cause: Throwable? = null) :
    SdkException(message, cause)

/** Configuration error. */
class ConfigException(message: String) : SdkException(message)

/** Platform error - module not available on current platform. */
class PlatformException(
    val moduleName: String,
    val currentPlatform: String,
    val supportedPlatforms: List<String>
) : SdkException(
    "Module \"$moduleName\" is not available on platform \"$currentPlatform\". " +
            "Supported platforms: ${supportedPlatforms.joinToString(", ")}"
)

/**
 * Create a typed exception from a status code and payload.
 */
fun createApiException(status: Int, payload: ApiErrorPayload?): ApiException {
    val message = payload?.message ?: payload?.error ?: "Request failed with status $status"
    return when (status) {
        401 -> UnauthorizedException(message, payload)
        403 -> ForbiddenException(message, payload)
        404 -> NotFoundException(message, payload)
        429 -> RateLimitException(message, payload)
        in 500..599 -> ServerException(message, status, payload)
        else -> ApiException(message, status, payload?.error, payload)
    }
}
