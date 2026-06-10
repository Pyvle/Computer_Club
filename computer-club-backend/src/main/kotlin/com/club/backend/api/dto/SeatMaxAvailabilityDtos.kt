package com.club.backend.api.dto

import com.club.backend.domain.enum.SeatType
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class SeatMaxAvailabilityRequest(
    @field:NotNull
    val startAt: LocalDateTime
)

data class SeatMaxAvailabilityResponse(
    val seatId: Long,
    val label: String,
    val type: SeatType,
    val isAvailableAtStart: Boolean,
    val maxAvailableMinutes: Int?,
    val nextBookingStartsAt: LocalDateTime?
)
