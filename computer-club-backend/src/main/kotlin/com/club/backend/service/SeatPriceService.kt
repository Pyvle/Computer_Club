package com.club.backend.service

import com.club.backend.api.dto.SeatPriceResponse
import com.club.backend.repository.ClubSeatPriceRepository
import org.springframework.stereotype.Service

@Service
class SeatPriceService(
    private val repo: ClubSeatPriceRepository
) {

    fun list(clubId: Long): List<SeatPriceResponse> =
        repo.findAllByClub_Id(clubId)
            .sortedBy { it.seatType.name }
            .map { SeatPriceResponse(seatType = it.seatType.name, pricePerHourRub = it.pricePerHourRub) }
}
