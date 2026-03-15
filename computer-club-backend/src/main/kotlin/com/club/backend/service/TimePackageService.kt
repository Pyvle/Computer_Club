package com.club.backend.service

import com.club.backend.api.dto.TimePackageResponse
import com.club.backend.domain.entity.ClubTimePackageEntity
import com.club.backend.repository.ClubTimePackageRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

@Service
class TimePackageService(
    private val repo: ClubTimePackageRepository
) {

    fun listActive(clubId: Long): List<TimePackageResponse> =
        repo.findAllByClub_IdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId)
            .map { it.toDto() }

    private fun ClubTimePackageEntity.toDto() = TimePackageResponse(
        id = id!!,
        name = name,
        hours = hours,
        pricePerHourRub = pricePerHourRub,
        totalPriceRub = pricePerHourRub * hours,
        availableFrom = availableFrom?.format(TIME_FMT),
        availableTo = availableTo?.format(TIME_FMT)
    )
}
