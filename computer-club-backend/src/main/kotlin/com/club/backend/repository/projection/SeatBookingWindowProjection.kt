package com.club.backend.repository.projection

import java.time.LocalDateTime

interface SeatBookingWindowProjection {
    fun getSeatId(): Long
    fun getStartAt(): LocalDateTime
    fun getEndAt(): LocalDateTime
}
