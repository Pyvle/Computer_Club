package com.club.backend.repository

import com.club.backend.domain.entity.IdempotencyKeyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface IdempotencyKeyRepository : JpaRepository<IdempotencyKeyEntity, String> {
    fun findByIdAndUser_IdAndEndpoint(id: String, userId: Long, endpoint: String): IdempotencyKeyEntity?
}
