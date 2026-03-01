package com.club.backend.service

import com.club.backend.api.error.ConflictException
import com.club.backend.domain.entity.IdempotencyKeyEntity
import com.club.backend.repository.IdempotencyKeyRepository
import com.club.backend.repository.UserRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class IdempotencyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    /**
     * Выполняет [action] не более одного раза для (userId, endpoint, idempotencyKey).
     * При повторном использовании ключа возвращает ранее сохранённый ответ.
     * Если ключ существует, но хэш запроса отличается — бросает ConflictException.
     */
    @Transactional
    fun <T : Any> execute(
        userId: Long,
        endpoint: String,
        idempotencyKey: String,
        requestPayloadForHash: Any,
        responseClass: Class<T>,
        ttlHours: Long = 24,
        action: () -> T
    ): T {
        val requestHash = sha256Hex(objectMapper.writeValueAsBytes(requestPayloadForHash))

        val existing = idempotencyKeyRepository.findById(idempotencyKey).orElse(null)
        if (existing != null) {
            // проверяем, что ключ принадлежит тому же пользователю и эндпоинту
            if (existing.user.id != userId || existing.endpoint != endpoint) {
                throw ConflictException("Idempotency key already used")
            }
            if (existing.requestHash != requestHash) {
                throw ConflictException("Idempotency key reused with different request")
            }
            return objectMapper.treeToValue(existing.responseBody, responseClass)
        }

        val result = action()
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val responseNode: JsonNode = objectMapper.valueToTree(result)

        val entity = IdempotencyKeyEntity(
            id = idempotencyKey,
            user = user,
            endpoint = endpoint,
            requestHash = requestHash,
            statusCode = HttpStatus.OK.value(),
            responseBody = responseNode,
            createdAt = OffsetDateTime.now(),
            expiresAt = OffsetDateTime.now().plus(ttlHours, ChronoUnit.HOURS)
        )
        idempotencyKeyRepository.save(entity)

        return result
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
