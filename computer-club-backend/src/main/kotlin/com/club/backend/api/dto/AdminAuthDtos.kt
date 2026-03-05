package com.club.backend.api.dto

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(
    @field:NotBlank val phone: String,
    @field:NotBlank val password: String
)
