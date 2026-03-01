package com.club.backend.api.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class SeatAvailabilityRequest(
    @field:NotNull
    val startAt: LocalDateTime,

    @field:NotNull
    val endAt: LocalDateTime
)