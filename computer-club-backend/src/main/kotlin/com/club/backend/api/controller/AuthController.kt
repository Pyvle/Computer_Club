package com.club.backend.api.controller

import com.club.backend.api.dto.*
import com.club.backend.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/otp/request")
    fun requestOtp(@Valid @RequestBody dto: OtpRequestDto): OtpRequestResponse {
        val result = authService.requestOtp(dto.phone)
        return OtpRequestResponse(
            challengeId = result.challengeId,
            resendInSeconds = result.resendInSeconds,
            debugCode = result.debugCode
        )
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(
        @Valid @RequestBody dto: OtpVerifyDto,
        req: HttpServletRequest
    ): AuthTokensResponse {
        val pair = authService.verifyOtp(
            challengeId = dto.challengeId,
            code = dto.code,
            userAgent = req.getHeader("User-Agent"),
            ip = req.remoteAddr
        )
        return AuthTokensResponse(pair.accessToken, pair.refreshToken)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody dto: RefreshDto,
        req: HttpServletRequest
    ): AuthTokensResponse {
        val pair = authService.refresh(
            refreshToken = dto.refreshToken,
            userAgent = req.getHeader("User-Agent"),
            ip = req.remoteAddr
        )
        return AuthTokensResponse(pair.accessToken, pair.refreshToken)
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody dto: LogoutDto) {
        authService.logout(dto.refreshToken)
    }
}
