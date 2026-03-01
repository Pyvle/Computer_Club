package com.club.backend.service

import com.club.backend.api.dto.AdminLoginRequest
import com.club.backend.domain.entity.AuthSessionEntity
import com.club.backend.domain.entity.GlobalRole
import com.club.backend.repository.AuthSessionRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import com.club.backend.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
class AdminAuthService(
    private val userRepository: UserRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: BCryptPasswordEncoder,
    @Value("\${app.jwt.refresh-exp-days}") private val refreshExpDays: Long
) {

    @Transactional
    fun login(request: AdminLoginRequest, userAgent: String?, ip: String?): TokenPair {
        val user = userRepository.findByUsername(request.username).orElse(null)

        // единое сообщение при любой ошибке — без подсказок атакующему
        if (user == null || user.passwordHash == null || !user.isActive) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        // проверка доступа к панели: GLOBAL_ADMIN или любой club staff
        val hasAccess = user.globalRole == GlobalRole.GLOBAL_ADMIN
            || clubStaffRepository.existsByUserId(user.id!!)
        if (!hasAccess) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        val access = jwtService.generateAccessToken(user.id!!, user.globalRole)
        val refresh = jwtService.generateRefreshToken(user.id!!)

        authSessionRepository.save(
            AuthSessionEntity(
                user = user,
                refreshTokenHash = sha256(refresh),
                userAgent = userAgent?.take(255),
                ip = ip?.take(64),
                expiresAt = LocalDateTime.now().plusDays(refreshExpDays)
            )
        )

        return TokenPair(access, refresh)
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
