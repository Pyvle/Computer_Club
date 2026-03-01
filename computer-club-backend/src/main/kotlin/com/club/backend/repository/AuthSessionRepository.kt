package com.club.backend.repository

import com.club.backend.domain.entity.AuthSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

interface AuthSessionRepository : JpaRepository<AuthSessionEntity, Long> {
    fun findByRefreshTokenHash(refreshTokenHash: String): Optional<AuthSessionEntity>
    fun deleteAllByExpiresAtBefore(expiresAt: LocalDateTime): Long
}
