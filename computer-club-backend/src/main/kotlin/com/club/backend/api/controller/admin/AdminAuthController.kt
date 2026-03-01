package com.club.backend.api.controller.admin

import com.club.backend.api.dto.AdminLoginRequest
import com.club.backend.api.dto.AuthTokensResponse
import com.club.backend.api.dto.LogoutDto
import com.club.backend.api.dto.RefreshDto
import com.club.backend.service.AdminAuthService
import com.club.backend.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/auth")
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: AdminLoginRequest,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        val (access, refresh) = adminAuthService.login(
            request,
            httpRequest.getHeader("User-Agent"),
            httpRequest.remoteAddr
        )
        return AuthTokensResponse(access, refresh)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody @Valid body: RefreshDto,
        httpRequest: HttpServletRequest
    ): AuthTokensResponse {
        val (access, refresh) = authService.refresh(
            body.refreshToken,
            httpRequest.getHeader("User-Agent"),
            httpRequest.remoteAddr
        )
        return AuthTokensResponse(access, refresh)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestBody @Valid body: LogoutDto) {
        authService.logout(body.refreshToken)
    }
}
