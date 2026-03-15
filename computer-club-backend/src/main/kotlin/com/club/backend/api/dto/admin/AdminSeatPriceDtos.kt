package com.club.backend.api.dto.admin

data class AdminSeatPriceResponse(
    val seatType: String,
    val pricePerHourRub: Int
)

data class UpsertSeatPriceRequest(
    val seatType: String,
    val pricePerHourRub: Int
)
