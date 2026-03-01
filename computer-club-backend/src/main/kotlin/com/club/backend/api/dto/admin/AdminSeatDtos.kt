package com.club.backend.api.dto.admin

import com.club.backend.domain.enum.SeatType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AdminSeatResponse(
    val id: Long,
    val label: String,
    val type: SeatType,
    val isActive: Boolean,
    val sortOrder: Int
)

data class CreateSeatRequest(
    @field:NotBlank
    val label: String,
    @field:NotNull
    val type: SeatType,
    val sortOrder: Int = 0
)

data class UpdateSeatRequest(
    @field:NotBlank
    val label: String,
    @field:NotNull
    val type: SeatType,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
