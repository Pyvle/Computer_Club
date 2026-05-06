package com.club.backend.service

import com.club.backend.api.dto.ClubUserReportResponse
import com.club.backend.api.dto.CreateReportRequest
import com.club.backend.api.dto.PlatformMessageResponse
import com.club.backend.api.dto.UpdateReportStatusRequest
import com.club.backend.domain.entity.ClubUserReportEntity
import com.club.backend.domain.enum.ClubReportStatus
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
    fun getUserReports(clubId: Long, status: ClubReportStatus? = null): List<ClubUserReportResponse> {
        val reports = if (status != null)
            reportRepo.findAllByClubIdAndStatusOrderByCreatedAtDesc(clubId, status)
        else
            reportRepo.findAllByClubIdOrderByCreatedAtDesc(clubId)
        return reports.map {
            ClubUserReportResponse(
                id = it.id,
                userId = it.user.id!!,
                userPhone = it.user.phone,
                message = it.message,
                status = it.status,
                createdAt = it.createdAt
            )
        }
    }

    @Transactional
    fun updateStatus(clubId: Long, reportId: Long, req: UpdateReportStatusRequest): ClubUserReportResponse {
        val report = reportRepo.findById(reportId).orElseThrow { NoSuchElementException("Report $reportId not found") }
        require(report.club.id == clubId) { "Report does not belong to club $clubId" }
        report.status = req.status
        val saved = reportRepo.save(report)
        return ClubUserReportResponse(
            id = saved.id,
            userId = saved.user.id!!,
            userPhone = saved.user.phone,
            message = saved.message,
            status = saved.status,
            createdAt = saved.createdAt
        )
    }

    /** Предупреждения от GLOBAL_ADMIN для отображения владельцу клуба. */
    @Transactional(readOnly = true)
    fun getPlatformMessages(clubId: Long): List<PlatformMessageResponse> =
        warningRepo.findAllByClubIdOrderByCreatedAtDesc(clubId).map {
            PlatformMessageResponse(id = it.id, message = it.message, createdAt = it.createdAt)
        }
}
