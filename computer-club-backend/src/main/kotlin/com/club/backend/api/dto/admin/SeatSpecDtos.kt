package com.club.backend.api.dto.admin

data class SpecLine(
    val name: String,
    val value: String
)

data class SeatSpecResponse(
    val seatType: String,
    val title: String,
    val specs: List<SpecLine>
)

data class UpdateSeatSpecRequest(
    val title: String,
    val specs: List<SpecLine>
)
