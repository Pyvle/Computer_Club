package com.example.computerclub.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeatResponseDto(
    val id: Long,
    val label: String,
    val type: String
)

@Serializable
data class SeatAvailabilityRequestDto(
    val startAt: String,
    val endAt: String
)

@Serializable
data class SeatAvailabilityResponseDto(
    val seatId: Long,
    val label: String,
    val type: String,
    @SerialName("isAvailable") val isAvailable: Boolean
)
