package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "auth_sessions")
class AuthSessionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 128)
    var refreshTokenHash: String,

    @Column(name = "user_agent", length = 255)
    var userAgent: String? = null,

    @Column(name = "ip", length = 64)
    var ip: String? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null
)
