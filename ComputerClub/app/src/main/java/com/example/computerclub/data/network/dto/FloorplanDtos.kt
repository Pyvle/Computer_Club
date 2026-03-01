package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FloorplanResponseDto(
    val id: Long,
    val clubId: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val gridSize: Int,
    val status: String,
    val version: Int,
    val data: JsonElement,
    val updatedAt: String
)

@Serializable
data class FloorplanSeatAvailabilityDto(
    val seatId: Long,
    val isBusy: Boolean
)

@Serializable
data class FloorplanWithAvailabilityResponseDto(
    val floorplan: FloorplanResponseDto,
    val from: String,
    val to: String,
    val busySeatIds: List<Long> = emptyList(),
    val seats: List<FloorplanSeatAvailabilityDto> = emptyList()
)
