package com.club.backend.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminLoginRequest(
    @field:NotBlank val phone: String,
    @field:NotBlank val password: String
)

data class SetPasswordRequest(
    @field:NotBlank
    @field:Size(min = 6)
    val password: String
)

data class VerifyPasswordRequest(
    @field:NotBlank val password: String
)
