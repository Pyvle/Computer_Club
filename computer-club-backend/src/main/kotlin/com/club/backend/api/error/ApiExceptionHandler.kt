package com.club.backend.api.error

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.server.ResponseStatusException

data class ApiError(
    val code: String,
    val message: String
)

@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError("VALIDATION_ERROR", "Validation error")
        )

    @ExceptionHandler(EntityNotFoundException::class, NoSuchElementException::class)
    fun notFound(e: Exception) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError("NOT_FOUND", e.message ?: "Not found")
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError("BAD_REQUEST", e.message ?: "Bad request")
        )

    @ExceptionHandler(IllegalStateException::class)
    fun conflict(e: IllegalStateException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError("CONFLICT", e.message ?: "Conflict")
        )

    @ExceptionHandler(ResponseStatusException::class)
    fun responseStatus(e: ResponseStatusException) =
        ResponseEntity.status(e.statusCode).body(
            ApiError(e.statusCode.value().toString(), e.reason ?: "Error")
        )

    @ExceptionHandler(Exception::class)
    fun unknown(e: Exception): ResponseEntity<ApiError> {
        log.error("Unhandled exception", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError("INTERNAL_ERROR", "Unexpected error")
        )
    }

    @ExceptionHandler(BlockedInClubException::class)
    fun blocked(e: BlockedInClubException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiError("BLOCKED_IN_CLUB", e.message ?: "Blocked in this club")
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun denied(e: AccessDeniedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiError("FORBIDDEN", "Forbidden")
        )
}