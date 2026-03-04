package com.club.backend.api.dto.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class AdminUserResponse(
    val id: Long,
    val phone: String?,
    val username: String,
    val isActive: Boolean,
    val globalRole: String,
    val hasPassword: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateUserRequest(
    @field:NotBlank
    @field:Size(min = 3)
    val username: String,
    @field:NotBlank
    @field:Size(min = 6)
    val password: String,
    val globalRole: String = "USER"
)

data class SetActiveRequest(val isActive: Boolean)
