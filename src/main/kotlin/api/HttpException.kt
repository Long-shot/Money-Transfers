package api

import io.ktor.http.HttpStatusCode

/**
 * Describes checked exceptions that are being forwarded accountTo user
 */
class HttpException(val code: HttpStatusCode, val description: String = code.description) :
    RuntimeException(description)