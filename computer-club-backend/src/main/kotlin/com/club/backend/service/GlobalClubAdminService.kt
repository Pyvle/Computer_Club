package com.club.backend.service

import com.club.backend.api.dto.admin.BlockClubRequest
import com.club.backend.api.dto.admin.ClubWarningRequest
import com.club.backend.api.dto.admin.ClubWarningResponse
import com.club.backend.api.dto.admin.GlobalClubResponse
import com.club.backend.domain.entity.ClubWarningEntity
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubWarningRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GlobalClubAdminService(
    private val clubRepo: ClubRepository,
    private val warningRepo: ClubWarningRepository
) {

    fun listAll(): List<GlobalClubResponse> =
        clubRepo.findAllByOrderByIdDesc().map { it.toResponse() }

    @Transactional
    fun blockClub(clubId: Long, req: BlockClubRequest) {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        club.isBlocked = true
        club.blockReason = req.reason
    }

    @Transactional
    fun unblockClub(clubId: Long) {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        club.isBlocked = false
        club.blockReason = null
    }

    @Transactional
    fun deleteClub(clubId: Long) {
        if (!clubRepo.existsById(clubId)) throw NoSuchElementException("Club $clubId not found")
        clubRepo.deleteById(clubId)
    }

    @Transactional
    fun addWarning(clubId: Long, adminId: Long, req: ClubWarningRequest): ClubWarningResponse {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        val warning = warningRepo.save(
            ClubWarningEntity(club = club, message = req.message, createdBy = adminId)
        )
        return warning.toResponse()
    }

    fun getWarnings(clubId: Long): List<ClubWarningResponse> =
        warningRepo.findAllByClubIdOrderByCreatedAtDesc(clubId).map { it.toResponse() }

    private fun com.club.backend.domain.entity.ClubEntity.toResponse() = GlobalClubResponse(
        id = id!!,
        name = name,
        addressShort = addressShort,
        addressFull = addressFull,
        description = description,
        imageUrl = imageUrl,
        isActive = isActive,
        isBlocked = isBlocked,
        blockReason = blockReason,
        createdAt = createdAt.toString()
    )

    private fun ClubWarningEntity.toResponse() = ClubWarningResponse(
        id = id,
        message = message,
        createdBy = createdBy,
        createdAt = createdAt
    )
}
