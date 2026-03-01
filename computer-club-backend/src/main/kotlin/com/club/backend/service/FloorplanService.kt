package com.club.backend.service

import com.club.backend.api.dto.FloorplanResponse
import com.club.backend.api.dto.FloorplanSeatAvailabilityDto
import com.club.backend.api.dto.FloorplanWithAvailabilityResponse
import com.club.backend.api.dto.SeatAvailabilityResponse
import com.club.backend.domain.enum.FloorplanStatus
import com.club.backend.repository.ClubFloorplanRepository
import com.club.backend.repository.BookingRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class FloorplanService(
    private val clubFloorplanRepository: ClubFloorplanRepository,
    private val bookingRepository: BookingRepository
) {
    fun getPublished(clubId: Long): FloorplanResponse {
        val fp = clubFloorplanRepository.findByClubIdAndStatus(clubId, FloorplanStatus.PUBLISHED)
            ?: throw EntityNotFoundException("Published floorplan not found for club $clubId")
        return FloorplanResponse(
            id = requireNotNull(fp.id),
            clubId = fp.clubId,
            name = fp.name,
            width = fp.width,
            height = fp.height,
            gridSize = fp.gridSize,
            status = fp.status,
            version = fp.version,
            data = fp.data,
            updatedAt = fp.updatedAt.toString()
        )
    }

    fun getPublishedWithAvailability(
        clubId: Long,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime
    ): FloorplanWithAvailabilityResponse {
        require(to.isAfter(from)) { "to must be after from" }

        val fpEntity = clubFloorplanRepository.findByClubIdAndStatus(clubId, FloorplanStatus.PUBLISHED)
            ?: throw EntityNotFoundException("Published floorplan not found for club $clubId")

        val floorplan = FloorplanResponse(
            id = requireNotNull(fpEntity.id),
            clubId = fpEntity.clubId,
            name = fpEntity.name,
            width = fpEntity.width,
            height = fpEntity.height,
            gridSize = fpEntity.gridSize,
            status = fpEntity.status,
            version = fpEntity.version,
            data = fpEntity.data,
            updatedAt = fpEntity.updatedAt.toString()
        )

        val busySeatIds = bookingRepository.findBusySeatIds(clubId, from, to)
            .map { it.getSeatId() }
            .distinct()
            .sorted()
        val busySet = busySeatIds.toHashSet()

        // извлекаем seatId из JSON схемы зала
        val seatIdsInPlan = mutableSetOf<Long>()
        val items = floorplan.data.get("items")
        if (items != null && items.isArray) {
            for (item in items) {
                val type = item.get("type")?.asText()
                if (type == "SEAT") {
                    // принимаем только явный numeric-тип: asLong() вернёт 0 для null/string, что даст ложный seatId
                    val seatNode = item.get("seatId")
                    if (seatNode != null && seatNode.isNumber) {
                        seatIdsInPlan.add(seatNode.longValue())
                    }
                }
            }
        }

        val seats = seatIdsInPlan.toList().sorted().map { seatId ->
            FloorplanSeatAvailabilityDto(
                seatId = seatId,
                isBusy = busySet.contains(seatId)
            )
        }

        return FloorplanWithAvailabilityResponse(
            floorplan = floorplan,
            from = from.toString(),
            to = to.toString(),
            busySeatIds = busySeatIds,
            seats = seats
        )
    }

}
