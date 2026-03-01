package com.club.backend.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class OtpRequestDto(
    @field:NotBlank
    @field:Pattern(regexp = "^\\+?[0-9]{10,15}$")
    val phone: String
)

data class OtpVerifyDto(
    val challengeId: Long,
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]{6}$")
    val code: String
)

data class RefreshDto(
    @field:NotBlank
    val refreshToken: String
)

data class LogoutDto(
    @field:NotBlank
    val refreshToken: String
)

data class OtpRequestResponse(
    val challengeId: Long,
    val resendInSeconds: Long,
    val debugCode: String? = null
)

data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String
)
