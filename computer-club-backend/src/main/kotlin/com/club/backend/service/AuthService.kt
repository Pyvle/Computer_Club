package com.club.backend.service

import com.club.backend.domain.entity.AuthSessionEntity
import com.club.backend.domain.entity.OtpChallengeEntity
import com.club.backend.domain.entity.UserEntity
import com.club.backend.repository.AuthSessionRepository
import com.club.backend.repository.OtpChallengeRepository
import com.club.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.random.Random

data class OtpRequestResult(val challengeId: Long, val resendInSeconds: Long, val debugCode: String?)
data class TokenPair(val accessToken: String, val refreshToken: String)

@Service
class AuthService(
    private val otpChallengeRepository: OtpChallengeRepository,
    private val userRepository: UserRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val jwtService: com.club.backend.security.JwtService,
    @Value("\${app.otp.ttl-seconds}") private val otpTtlSeconds: Long,
    @Value("\${app.otp.max-attempts}") private val otpMaxAttempts: Int,
    @Value("\${app.otp.resend-cooldown-seconds}") private val resendCooldownSeconds: Long,
    @Value("\${app.otp.max-requests-per-hour}") private val maxRequestsPerHour: Long,
    @Value("\${app.otp.limits-enabled}") private val limitsEnabled: Boolean,
    @Value("\${app.otp.debug-return-code}") private val debugReturnCode: Boolean,
    @Value("\${app.jwt.refresh-exp-days}") private val refreshExpDays: Long
) {

    @Transactional
    fun requestOtp(rawPhone: String): OtpRequestResult {
        val phone = normalizePhone(rawPhone)
        val now = LocalDateTime.now()

        if (limitsEnabled) {
            val cnt = otpChallengeRepository.countRequestsSince(phone, now.minusHours(1))
            require(cnt < maxRequestsPerHour) { "Too many OTP requests. Try later." }

            val latestPending = otpChallengeRepository.findLatestPendingByPhone(phone).firstOrNull()
            if (latestPending != null && latestPending.resendAvailableAt.isAfter(now)) {
                val wait = java.time.Duration.between(now, latestPending.resendAvailableAt).seconds
                return OtpRequestResult(latestPending.id!!, wait, null)
            }
        }

        val code = generateOtp6()
        val challenge = otpChallengeRepository.save(
            OtpChallengeEntity(
                phone = phone,
                codeHash = sha256("$phone:$code"),
                expiresAt = now.plusSeconds(otpTtlSeconds),
                attemptsLeft = if (limitsEnabled) otpMaxAttempts else 999999,
                resendAvailableAt = now.plusSeconds(if (limitsEnabled) resendCooldownSeconds else 0),
                status = "PENDING"
            )
        )

        return OtpRequestResult(
            challengeId = challenge.id!!,
            resendInSeconds = if (limitsEnabled) resendCooldownSeconds else 0,
            debugCode = if (debugReturnCode) code else null
        )
    }

    @Transactional
    fun verifyOtp(challengeId: Long, code: String, userAgent: String?, ip: String?): TokenPair {
        val ch = otpChallengeRepository.findByIdAndStatus(challengeId, "PENDING")
            .orElseThrow { EntityNotFoundException("Challenge not found") }

        val now = LocalDateTime.now()
        if (ch.expiresAt.isBefore(now)) {
            ch.status = "EXPIRED"
            otpChallengeRepository.save(ch)
            throw IllegalArgumentException("OTP expired")
        }

        val valid = sha256("${ch.phone}:$code") == ch.codeHash
        if (!valid) {
            if (limitsEnabled) {
                ch.attemptsLeft -= 1
                if (ch.attemptsLeft <= 0) ch.status = "CANCELED"
                otpChallengeRepository.save(ch)
            }
            throw IllegalArgumentException("Invalid OTP")
        }

        ch.status = "VERIFIED"
        ch.verifiedAt = now
        otpChallengeRepository.save(ch)

        val user = userRepository.findByPhone(ch.phone).orElseGet {
            userRepository.save(UserEntity(phone = ch.phone, username = "user_${ch.phone.takeLast(4)}"))
        }

        require(user.isActive) { "User is blocked" }

        val access = jwtService.generateAccessToken(user.id!!, user.globalRole)
        val refresh = jwtService.generateRefreshToken(user.id!!)
        saveSession(user, refresh, userAgent, ip)

        return TokenPair(access, refresh)
    }

    @Transactional
    fun refresh(refreshToken: String, userAgent: String?, ip: String?): TokenPair {
        require(jwtService.getType(refreshToken) == "refresh") { "Invalid token type" }

        val hash = sha256(refreshToken)
        val session = authSessionRepository.findByRefreshTokenHash(hash)
            .orElseThrow { EntityNotFoundException("Session not found") }

        require(session.revokedAt == null) { "Session revoked" }
        require(session.expiresAt.isAfter(LocalDateTime.now())) { "Refresh token expired" }
        require(session.user.isActive) { "User is blocked" }

        val userId = jwtService.getUserId(refreshToken)
        require(session.user.id == userId) { "Session mismatch" }

        // ротация токенов: отзываем старую сессию и выпускаем новую
        session.revokedAt = LocalDateTime.now()
        authSessionRepository.save(session)

        val newAccess = jwtService.generateAccessToken(userId, session.user.globalRole)
        val newRefresh = jwtService.generateRefreshToken(userId)
        saveSession(session.user, newRefresh, userAgent, ip)

        return TokenPair(newAccess, newRefresh)
    }

    @Transactional
    fun logout(refreshToken: String) {
        val hash = sha256(refreshToken)
        val session = authSessionRepository.findByRefreshTokenHash(hash).orElse(null) ?: return
        session.revokedAt = LocalDateTime.now()
        authSessionRepository.save(session)
    }

    private fun saveSession(user: UserEntity, refreshToken: String, userAgent: String?, ip: String?) {
        authSessionRepository.save(
            AuthSessionEntity(
                user = user,
                refreshTokenHash = sha256(refreshToken),
                userAgent = userAgent?.take(255),
                ip = ip?.take(64),
                expiresAt = LocalDateTime.now().plusDays(refreshExpDays)
            )
        )
    }

    private fun generateOtp6(): String = (100000 + Random.nextInt(900000)).toString()

    private fun normalizePhone(raw: String): String {
        val p = raw.trim().replace(" ", "")
        require(p.matches(Regex("^\\+?[0-9]{10,15}$"))) { "Invalid phone format" }
        return if (p.startsWith("+")) p else "+$p"
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
