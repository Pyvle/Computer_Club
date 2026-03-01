package com.club.backend.api.dto

import com.club.backend.domain.enum.SeatType

data class SeatResponse(
    val id: Long,
    val label: String,
    val type: SeatType
)