package com.club.backend.service

import com.club.backend.api.dto.SeatPriceResponse
import com.club.backend.repository.ClubSeatTypeSettingRepository
import org.springframework.stereotype.Service

@Service
class SeatPriceService(
    private val repo: ClubSeatTypeSettingRepository
) {

    fun list(clubId: Long): List<SeatPriceResponse> =
        repo.findAllByClub_Id(clubId)
            .filter { it.pricePerHourRub != null }
            .sortedBy { it.seatType.name }
            .map { SeatPriceResponse(seatType = it.seatType.name, pricePerHourRub = it.pricePerHourRub!!) }
}
