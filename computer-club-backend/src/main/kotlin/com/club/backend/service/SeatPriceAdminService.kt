package com.club.backend.service

import com.club.backend.api.dto.admin.AdminSeatPriceResponse
import com.club.backend.api.dto.admin.UpsertSeatPriceRequest
import com.club.backend.domain.entity.ClubSeatPriceEntity
import com.club.backend.domain.enum.SeatType
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubSeatPriceRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeatPriceAdminService(
    private val repo: ClubSeatPriceRepository,
    private val clubRepository: ClubRepository
) {

    fun list(clubId: Long): List<AdminSeatPriceResponse> =
        repo.findAllByClub_Id(clubId)
            .sortedBy { it.seatType.name }
            .map { AdminSeatPriceResponse(seatType = it.seatType.name, pricePerHourRub = it.pricePerHourRub) }

    @Transactional
    fun upsert(clubId: Long, req: UpsertSeatPriceRequest): AdminSeatPriceResponse {
        val seatType = runCatching { SeatType.valueOf(req.seatType) }
            .getOrElse { throw IllegalArgumentException("Unknown seat type: ${req.seatType}") }

        val existing = repo.findByClub_IdAndSeatType(clubId, seatType)
        val entity = if (existing.isPresent) {
            existing.get().also { it.pricePerHourRub = req.pricePerHourRub }
        } else {
            val club = clubRepository.findById(clubId)
                .orElseThrow { EntityNotFoundException("Club $clubId not found") }
            ClubSeatPriceEntity(club = club, seatType = seatType, pricePerHourRub = req.pricePerHourRub)
        }
        val saved = repo.save(entity)
        return AdminSeatPriceResponse(seatType = saved.seatType.name, pricePerHourRub = saved.pricePerHourRub)
    }
}
