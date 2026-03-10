package com.club.backend.service

import com.club.backend.api.dto.admin.ClubSettingsResponse
import com.club.backend.api.dto.admin.UpdateClubSettingsRequest
import com.club.backend.repository.ClubRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class ClubSettingsService(
    private val clubRepository: ClubRepository
) {

    fun get(clubId: Long): ClubSettingsResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found") }
        return club.toResponse()
    }

    @Transactional
    fun update(clubId: Long, req: UpdateClubSettingsRequest): ClubSettingsResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found") }
        club.name = req.name
        club.address = req.address
        club.locationText = req.locationText?.takeIf { it.isNotBlank() }
        club.description = req.description?.takeIf { it.isNotBlank() }
        club.isActive = req.isActive
        club.updatedAt = LocalDateTime.now()
        return club.toResponse()
    }

    private fun com.club.backend.domain.entity.ClubEntity.toResponse() = ClubSettingsResponse(
        id = id!!,
        name = name,
        address = address,
        locationText = locationText,
        description = description,
        isActive = isActive
    )
}
