package com.club.backend.api.dto

import com.club.backend.domain.enum.SeatType

data class SeatAvailabilityResponse(
    val seatId: Long,
    val label: String,
    val type: SeatType,
    val isAvailable: Boolean
)