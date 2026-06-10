package com.club.backend.security

import com.club.backend.domain.entity.GlobalRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtServiceTest {

    private val jwtService = JwtService(
        secret = "01234567890123456789012345678901",
        issuer = "computer-club-test",
        accessExpMinutes = 15,
        refreshExpDays = 30
    )

    @Test
    fun `access token contains user id role and access type`() {
        val token = jwtService.generateAccessToken(userId = 42L, globalRole = GlobalRole.GLOBAL_ADMIN)

        assertEquals(42L, jwtService.getUserId(token))
        assertEquals("access", jwtService.getType(token))
        assertEquals(GlobalRole.GLOBAL_ADMIN, jwtService.getGlobalRole(token))
    }

    @Test
    fun `refresh token contains user id and refresh type without admin role`() {
        val token = jwtService.generateRefreshToken(userId = 7L)

        assertEquals(7L, jwtService.getUserId(token))
        assertEquals("refresh", jwtService.getType(token))
        assertEquals(GlobalRole.USER, jwtService.getGlobalRole(token))
    }

    @Test
    fun `extractUserIdOrNull reads bearer token and ignores invalid headers`() {
        val token = jwtService.generateAccessToken(userId = 15L, globalRole = GlobalRole.USER)

        assertEquals(15L, jwtService.extractUserIdOrNull("Bearer $token"))
        assertNull(jwtService.extractUserIdOrNull(null))
        assertNull(jwtService.extractUserIdOrNull("Basic $token"))
        assertNull(jwtService.extractUserIdOrNull("Bearer not-a-token"))
    }
}
