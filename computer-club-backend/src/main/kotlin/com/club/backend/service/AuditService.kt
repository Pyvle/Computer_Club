package com.club.backend.service

import com.club.backend.domain.entity.AuditLogEntity
import com.club.backend.repository.AuditLogRepository
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.UserRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun log(
        actorUserId: Long,
        clubId: Long?,
        action: String,
        entityType: String,
        entityId: String?,
        before: Any? = null,
        after: Any? = null
    ) {
        val actor = userRepository.findById(actorUserId).orElseThrow { IllegalArgumentException("User not found") }
        val club = clubId?.let { clubRepository.findById(it).orElse(null) }

        val beforeNode: JsonNode? = before?.let { objectMapper.valueToTree(it) }
        val afterNode: JsonNode? = after?.let { objectMapper.valueToTree(it) }

        auditLogRepository.save(
            AuditLogEntity(
                actor = actor,
                club = club,
                action = action,
                entityType = entityType,
                entityId = entityId,
                beforeData = beforeNode,
                afterData = afterNode,
                createdAt = OffsetDateTime.now()
            )
        )
    }
}
