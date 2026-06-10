package com.club.backend.service

import com.club.backend.domain.entity.AuthSessionEntity
import com.club.backend.domain.entity.GlobalRole
import com.club.backend.domain.entity.OtpChallengeEntity
import com.club.backend.domain.entity.UserEntity
import com.club.backend.repository.AuthSessionRepository
import com.club.backend.repository.OtpChallengeRepository
import com.club.backend.repository.UserRepository
import com.club.backend.security.JwtService
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private val otpChallenges = linkedMapOf<Long, OtpChallengeEntity>()
    private val usersByPhone = linkedMapOf<String, UserEntity>()
    private val sessions = mutableListOf<AuthSessionEntity>()
    private var nextChallengeId = 10L
    private var nextUserId = 99L

    private val otpChallengeRepository: OtpChallengeRepository = proxyRepo { method, args ->
        when (method) {
            "countRequestsSince" -> {
                val phone = args[0] as String
                val fromTime = args[1] as LocalDateTime
                otpChallenges.values.count { it.phone == phone && !it.createdAt.isBefore(fromTime) }.toLong()
            }
            "findLatestPendingByPhone" -> {
                val phone = args[0] as String
                otpChallenges.values
                    .filter { it.phone == phone && it.status == "PENDING" }
                    .sortedByDescending { it.createdAt }
            }
            "save" -> {
                val challenge = args[0] as OtpChallengeEntity
                if (challenge.id == null) challenge.id = nextChallengeId++
                otpChallenges[challenge.id!!] = challenge
                challenge
            }
            "findByIdAndStatus" -> {
                val id = args[0] as Long
                val status = args[1] as String
                Optional.ofNullable(otpChallenges[id]?.takeIf { it.status == status })
            }
            else -> unsupported(method)
        }
    }
    private val userRepository: UserRepository = proxyRepo { method, args ->
        when (method) {
            "findByPhone" -> Optional.ofNullable(usersByPhone[args[0] as String])
            "save" -> {
                val user = args[0] as UserEntity
                if (user.id == null) user.id = nextUserId++
                usersByPhone[user.phone!!] = user
                user
            }
            else -> unsupported(method)
        }
    }
    private val authSessionRepository: AuthSessionRepository = proxyRepo { method, args ->
        when (method) {
            "save" -> {
                val session = args[0] as AuthSessionEntity
                sessions += session
                session
            }
            else -> unsupported(method)
        }
    }
    private val jwtService = JwtService(
        secret = "01234567890123456789012345678901",
        issuer = "computer-club-test",
        accessExpMinutes = 15,
        refreshExpDays = 30
    )
    private val service = AuthService(
        otpChallengeRepository = otpChallengeRepository,
        userRepository = userRepository,
        authSessionRepository = authSessionRepository,
        jwtService = jwtService,
        otpTtlSeconds = 180,
        otpMaxAttempts = 5,
        resendCooldownSeconds = 60,
        maxRequestsPerHour = 5,
        limitsEnabled = true,
        debugReturnCode = true,
        refreshExpDays = 30
    )

    @Test
    fun `requestOtp normalizes phone and returns debug code`() {
        val result = service.requestOtp("79998887766")
        val debugCode = result.debugCode
        val saved = otpChallenges.values.single()

        assertEquals("+79998887766", saved.phone)
        assertEquals("PENDING", saved.status)
        assertEquals(5, saved.attemptsLeft)
        assertEquals(10L, result.challengeId)
        assertEquals(60, result.resendInSeconds)
        assertNotNull(debugCode)
        assertEquals(6, debugCode.length)
    }

    @Test
    fun `verifyOtp creates user and session for valid code`() {
        val phone = "+79990001122"
        val code = "123456"
        val challenge = OtpChallengeEntity(
            id = 11L,
            phone = phone,
            codeHash = sha256("$phone:$code"),
            expiresAt = LocalDateTime.now().plusMinutes(3),
            attemptsLeft = 5,
            resendAvailableAt = LocalDateTime.now().minusSeconds(1)
        )
        otpChallenges[11L] = challenge

        val result = service.verifyOtp(11L, code, "JUnit", "127.0.0.1")

        assertEquals("VERIFIED", challenge.status)
        assertNotNull(challenge.verifiedAt)
        assertEquals(99L, jwtService.getUserId(result.accessToken))
        assertEquals(99L, jwtService.getUserId(result.refreshToken))
        assertEquals("access", jwtService.getType(result.accessToken))
        assertEquals("refresh", jwtService.getType(result.refreshToken))

        val session = sessions.single()
        assertEquals(99L, session.user.id)
        assertTrue(session.refreshTokenHash.isNotBlank())
        assertEquals("JUnit", session.userAgent)
        assertEquals("127.0.0.1", session.ip)
        assertEquals(GlobalRole.USER, jwtService.getGlobalRole(result.accessToken))
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
