package com.club.backend.service

import com.club.backend.api.dto.admin.AuditLogResponse
import com.club.backend.repository.AuditLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuditQueryService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional(readOnly = true)
    fun latestForClub(clubId: Long, limit: Int): List<AuditLogResponse> {
        val rows = auditLogRepository.findLatestByClubId(clubId).take(limit.coerceIn(1, 200))
        return rows.map {
            AuditLogResponse(
                id = it.id!!,
                createdAt = it.createdAt.toString(),
                actorUserId = it.actor.id!!,
                action = it.action,
                entityType = it.entityType,
                entityId = it.entityId,
                before = it.beforeData?.let { node -> objectMapper.convertValue(node, Any::class.java) },
                after = it.afterData?.let { node -> objectMapper.convertValue(node, Any::class.java) }
            )
        }
    }
}
