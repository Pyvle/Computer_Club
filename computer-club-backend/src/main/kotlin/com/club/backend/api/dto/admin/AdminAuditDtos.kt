package com.club.backend.api.dto.admin

data class AuditLogResponse(
    val id: Long,
    val createdAt: String,
    val actorUserId: Long,
    val actorPhone: String?,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val before: Any?,
    val after: Any?
)
