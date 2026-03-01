package com.club.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.club.backend.domain.entity.GlobalRole
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.issuer}") private val issuer: String,
    @Value("\${app.jwt.access-exp-minutes}") private val accessExpMinutes: Long,
    @Value("\${app.jwt.refresh-exp-days}") private val refreshExpDays: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateAccessToken(userId: Long, globalRole: GlobalRole): String =
        generate(userId, "access", accessExpMinutes, ChronoUnit.MINUTES, globalRole)

    fun generateRefreshToken(userId: Long): String =
        generate(userId, "refresh", refreshExpDays, ChronoUnit.DAYS, null)

    private fun generate(userId: Long, type: String, amount: Long, unit: ChronoUnit, globalRole: GlobalRole?): String {
        val now = Instant.now()
        val exp = now.plus(amount, unit)
        val builder = Jwts.builder()
            .subject(userId.toString())
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim("type", type)

        if (type == "access" && globalRole != null) {
            builder.claim("gr", globalRole.name)
        }

        return builder.signWith(key).compact()
    }

    fun parse(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .payload

    fun getUserId(token: String): Long = parse(token).subject.toLong()
    fun getType(token: String): String = parse(token)["type"] as String

    fun getGlobalRole(token: String): GlobalRole {
        val v = parse(token)["gr"]?.toString() ?: GlobalRole.USER.name
        return runCatching { GlobalRole.valueOf(v) }.getOrDefault(GlobalRole.USER)
    }

    /**
     * Извлекает userId из заголовка без выброса исключения — для публичных эндпоинтов,
     * которые всё равно хотят применять клубные блоки при авторизованном пользователе.
     */
    fun extractUserIdOrNull(authorizationHeader: String?): Long? {
        val header = authorizationHeader?.trim() ?: return null
        if (!header.startsWith("Bearer ", ignoreCase = true)) return null
        val token = header.substringAfter("Bearer ").trim()
        return runCatching { getUserId(token) }.getOrNull()
    }
}

