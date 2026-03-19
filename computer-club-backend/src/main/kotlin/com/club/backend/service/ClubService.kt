package com.club.backend.service

import com.club.backend.api.dto.AvailableClubResponse
import com.club.backend.api.dto.ClubResponse
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubUserBlockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val clubUserBlockRepository: ClubUserBlockRepository
) {

    @Transactional(readOnly = true)
    fun getAllActive(): List<ClubResponse> {
        return clubRepository.findAllByIsActiveTrueAndIsBlockedFalseOrderByIdAsc().map {
            ClubResponse(
                id = it.id!!,
                name = it.name,
                address = it.addressShort,
                locationText = it.locationText,
                description = it.description,
                imageUrl = it.imageUrl,
                latitude = it.latitude,
                longitude = it.longitude
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
                longitude = club.longitude
            )
        }
    }
}