package com.club.backend.service

import com.club.backend.api.dto.ClubUserReportResponse
import com.club.backend.api.dto.CreateReportRequest
import com.club.backend.api.dto.PlatformMessageResponse
import com.club.backend.domain.entity.ClubUserReportEntity
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubUserReportRepository
import com.club.backend.repository.ClubWarningRepository
import com.club.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubUserReportService(
    private val reportRepo: ClubUserReportRepository,
    private val warningRepo: ClubWarningRepository,
    private val clubRepo: ClubRepository,
    private val userRepo: UserRepository
) {

    @Transactional
    fun submit(clubId: Long, userId: Long, req: CreateReportRequest) {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        val user = userRepo.findById(userId).orElseThrow { NoSuchElementException("User $userId not found") }
        reportRepo.save(ClubUserReportEntity(club = club, user = user, message = req.message))
    }

    @Transactional(readOnly = true)
    fun getUserReports(clubId: Long): List<ClubUserReportResponse> =
        reportRepo.findAllByClubIdOrderByCreatedAtDesc(clubId).map {
            ClubUserReportResponse(
                id = it.id,
                userId = it.user.id!!,
                userPhone = it.user.phone,
                message = it.message,
                createdAt = it.createdAt
            )
        }

    /** Предупреждения от GLOBAL_ADMIN для отображения владельцу клуба. */
    @Transactional(readOnly = true)
    fun getPlatformMessages(clubId: Long): List<PlatformMessageResponse> =
        warningRepo.findAllByClubIdOrderByCreatedAtDesc(clubId).map {
            PlatformMessageResponse(id = it.id, message = it.message, createdAt = it.createdAt)
        }
}
