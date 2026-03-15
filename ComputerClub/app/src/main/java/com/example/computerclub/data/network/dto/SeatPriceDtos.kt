package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SeatPriceResponseDto(
    val seatType: String,
    val pricePerHourRub: Int
)
