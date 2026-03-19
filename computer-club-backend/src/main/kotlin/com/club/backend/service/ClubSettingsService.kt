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
    private val clubRepository: ClubRepository,
    private val auditService: AuditService,
    private val fileStorageService: FileStorageService
) {

    fun get(clubId: Long): ClubSettingsResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found") }
        return club.toResponse()
    }

    @Transactional
    fun update(clubId: Long, req: UpdateClubSettingsRequest, actorUserId: Long): ClubSettingsResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found") }
        val before = club.toResponse()
        club.name = req.name
        club.addressFull = req.addressFull
        club.addressShort = req.addressShort
        club.locationText = req.locationText?.takeIf { it.isNotBlank() }
        club.description = req.description?.takeIf { it.isNotBlank() }
        val newImageUrl = req.imageUrl?.takeIf { it.isNotBlank() }
        if (newImageUrl != club.imageUrl) {
            fileStorageService.deleteIfLocal(club.imageUrl)
            club.imageUrl = newImageUrl
        }
        club.isActive = req.isActive
        club.latitude = req.latitude
        club.longitude = req.longitude
        club.updatedAt = LocalDateTime.now()
        val after = club.toResponse()
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "CLUB_SETTINGS_UPDATE",
            entityType = "Club",
            entityId = clubId.toString(),
            before = before,
            after = after
        )
        return after
    }

    private fun com.club.backend.domain.entity.ClubEntity.toResponse() = ClubSettingsResponse(
        id = id!!,
        name = name,
        addressFull = addressFull,
        addressShort = addressShort,
        locationText = locationText,
        description = description,
        imageUrl = imageUrl,
        isActive = isActive,
        latitude = latitude,
        longitude = longitude
    )
}
