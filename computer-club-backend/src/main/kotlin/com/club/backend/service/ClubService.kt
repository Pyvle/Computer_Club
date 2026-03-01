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
        return clubRepository.findAllByIsActiveTrueOrderByIdAsc().map {
            ClubResponse(
                id = it.id!!,
                name = it.name,
                address = it.address,
                locationText = it.locationText,
                description = it.description
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

        return clubRepository.findAllByIsActiveTrueOrderByIdAsc().map { club ->
            val b = activeBlocks[club.id!!]
            AvailableClubResponse(
                id = club.id!!,
                name = club.name,
                address = club.address,
                locationText = club.locationText,
                description = club.description,
                isBlocked = b != null,
                blockReason = b?.reason,
                blockedUntil = b?.blockedUntil
            )
        }
    }
}