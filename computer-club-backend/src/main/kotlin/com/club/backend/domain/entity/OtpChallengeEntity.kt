package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "otp_challenges")
class OtpChallengeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 32)
    var phone: String,

    @Column(name = "code_hash", nullable = false, length = 128)
    var codeHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "attempts_left", nullable = false)
    var attemptsLeft: Int = 5,

    @Column(name = "resend_available_at", nullable = false)
    var resendAvailableAt: LocalDateTime,

    @Column(nullable = false, length = 16)
    var status: String = "PENDING", // PENDING/VERIFIED/EXPIRED/CANCELED

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null
)
