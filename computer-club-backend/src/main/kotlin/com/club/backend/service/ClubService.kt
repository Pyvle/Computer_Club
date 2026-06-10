package com.club.backend.service

import com.club.backend.api.dto.AvailableClubResponse
import com.club.backend.api.dto.ClubResponse
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubSeatTypeSettingRepository
import com.club.backend.repository.ClubUserBlockRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val clubUserBlockRepository: ClubUserBlockRepository,
    private val clubSeatTypeSettingRepository: ClubSeatTypeSettingRepository
) {

    @Transactional(readOnly = true)
    fun getById(clubId: Long): ClubResponse {
        val club = clubRepository.findById(clubId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found")
        }
        val minPrice = clubSeatTypeSettingRepository.findAllByClub_Id(clubId)
            .mapNotNull { it.pricePerHourRub }
            .minOrNull()
        return ClubResponse(
            id = club.id!!,
            name = club.name,
            address = club.addressShort,
            locationText = club.locationText,
            description = club.description,
            imageUrl = club.imageUrl,
            latitude = club.latitude,
            longitude = club.longitude,
            minPricePerHourRub = minPrice
        )
    }

    @Transactional(readOnly = true)
    fun getAllActive(): List<ClubResponse> {
        val minPriceByClub = minPriceMap()
        return clubRepository.findAllByIsActiveTrueAndIsBlockedFalseOrderByIdAsc().map {
            ClubResponse(
                id = it.id!!,
                name = it.name,
                address = it.addressShort,
                locationText = it.locationText,
                description = it.description,
                imageUrl = it.imageUrl,
                latitude = it.latitude,
                longitude = it.longitude,
                minPricePerHourRub = minPriceByClub[it.id!!]
            )
        }
    }

    /**
     * Для UX: фронт сразу понимает, какие клубы недоступны пользователю из‑за бана.
     * Backend всё равно проверяет бан на cart/checkout и клубных ресурсах.
     */
    @Transactional(readOnly = true)
    fun getAvailableForUser(userId: Long): List<AvailableClubResponse> {
        val now = LocalDateTime.now()
        val activeBlocks = clubUserBlockRepository.findActiveBlocksForUser(userId, now)
            .associateBy { it.club.id!! }
        val minPriceByClub = minPriceMap()

        return clubRepository.findAllByIsActiveTrueAndIsBlockedFalseOrderByIdAsc().map { club ->
            val b = activeBlocks[club.id!!]
            AvailableClubResponse(
                id = club.id!!,
                name = club.name,
                address = club.addressShort,
                locationText = club.locationText,
                description = club.description,
                imageUrl = club.imageUrl,
                isBlocked = b != null,
                blockReason = b?.reason,
                blockedUntil = b?.blockedUntil,
                latitude = club.latitude,
                longitude = club.longitude,
                minPricePerHourRub = minPriceByClub[club.id!!]
            )
        }
    }

    // один запрос на все клубы, не N запросов
    private fun minPriceMap(): Map<Long, Int> =
        clubSeatTypeSettingRepository.findMinPricePerClub()
            .associate { row -> (row[0] as Long) to (row[1] as Int) }
}
