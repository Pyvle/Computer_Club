package com.club.backend.service

import com.club.backend.api.dto.ClubUserReportResponse
import com.club.backend.api.dto.CreateReportRequest
import com.club.backend.api.dto.PlatformMessageResponse
import com.club.backend.api.dto.UpdateReportStatusRequest
import com.club.backend.domain.entity.ClubMessageEntity
import com.club.backend.domain.entity.ClubMessageType
import com.club.backend.domain.enum.ClubReportStatus
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubMessageRepository
import com.club.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubUserReportService(
    private val messageRepo: ClubMessageRepository,
    private val clubRepo: ClubRepository,
    private val userRepo: UserRepository
) {

    @Transactional
    fun submit(clubId: Long, userId: Long, req: CreateReportRequest) {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        val user = userRepo.findById(userId).orElseThrow { NoSuchElementException("User $userId not found") }
        messageRepo.save(
            ClubMessageEntity(
                club = club,
                author = user,
                messageType = ClubMessageType.USER_REPORT,
                message = req.message,
                status = ClubReportStatus.NEW
            )
        )
    }

    @Transactional(readOnly = true)
    fun getUserReports(clubId: Long, status: ClubReportStatus? = null): List<ClubUserReportResponse> {
        val reports = if (status != null)
            messageRepo.findAllByClub_IdAndMessageTypeAndStatusOrderByCreatedAtDesc(
                clubId,
                ClubMessageType.USER_REPORT,
                status
            )
        else
            messageRepo.findAllByClub_IdAndMessageTypeOrderByCreatedAtDesc(clubId, ClubMessageType.USER_REPORT)
        return reports.map {
            ClubUserReportResponse(
                id = it.id!!,
                userId = it.author!!.id!!,
                userPhone = it.author!!.phone,
                message = it.message,
                status = it.status!!,
                createdAt = it.createdAt
            )
        }
    }

    @Transactional
    fun updateStatus(clubId: Long, reportId: Long, req: UpdateReportStatusRequest): ClubUserReportResponse {
        val report = messageRepo.findById(reportId).orElseThrow { NoSuchElementException("Report $reportId not found") }
        require(report.messageType == ClubMessageType.USER_REPORT) { "Report $reportId not found" }
        require(report.club.id == clubId) { "Report does not belong to club $clubId" }
        report.status = req.status
        val saved = messageRepo.save(report)
        return ClubUserReportResponse(
            id = saved.id!!,
            userId = saved.author!!.id!!,
            userPhone = saved.author!!.phone,
            message = saved.message,
            status = saved.status!!,
            createdAt = saved.createdAt
        )
    }

    /** Предупреждения от GLOBAL_ADMIN для отображения владельцу клуба. */
    @Transactional(readOnly = true)
    fun getPlatformMessages(clubId: Long): List<PlatformMessageResponse> =
        messageRepo.findAllByClub_IdAndMessageTypeOrderByCreatedAtDesc(clubId, ClubMessageType.PLATFORM_WARNING).map {
            PlatformMessageResponse(id = it.id!!, message = it.message, createdAt = it.createdAt)
        }
}
