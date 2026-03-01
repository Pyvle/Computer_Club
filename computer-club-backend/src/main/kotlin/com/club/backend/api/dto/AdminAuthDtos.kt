package com.club.backend.api.dto

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)
