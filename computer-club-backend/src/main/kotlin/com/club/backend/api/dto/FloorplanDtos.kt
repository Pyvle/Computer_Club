package com.club.backend.api.dto

import com.club.backend.domain.enum.FloorplanStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import com.fasterxml.jackson.databind.JsonNode

data class FloorplanSummaryResponse(
    val id: Long,
    val clubId: Long,
    val name: String,
    val status: FloorplanStatus,
    val version: Int,
    val updatedAt: String
)

data class FloorplanResponse(
    val id: Long,
    val clubId: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val gridSize: Int,
    val status: FloorplanStatus,
    val version: Int,
    val data: JsonNode,
    val updatedAt: String
)

data class CreateFloorplanRequest(
    @field:NotBlank
    val name: String,
    @field:NotNull @field:Min(100)
    val width: Int,
    @field:NotNull @field:Min(100)
    val height: Int,
    @field:NotNull @field:Min(1)
    val gridSize: Int = 10
)

data class UpdateFloorplanRequest(
    @field:NotBlank
    val name: String,
    @field:NotNull @field:Min(100)
    val width: Int,
    @field:NotNull @field:Min(100)
    val height: Int,
    @field:NotNull @field:Min(1)
    val gridSize: Int,
    /** Оптимистичная блокировка — предотвращает конфликты при параллельном редактировании. */
    @field:NotNull
    val version: Int,

    @field:NotNull
    val data: JsonNode
)

data class CloneFloorplanRequest(
    @field:NotBlank
    val name: String
)

data class PublishFloorplanResponse(
    val publishedId: Long
)

data class FloorplanWithAvailabilityResponse(
    val floorplan: FloorplanResponse,
    val from: String,
    val to: String,
    val busySeatIds: List<Long>,
    val seats: List<FloorplanSeatAvailabilityDto>
)
