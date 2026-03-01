package com.club.backend.service

import com.club.backend.api.dto.*
import com.club.backend.domain.entity.*
import com.club.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ClubApplicationService(
    private val clubApplicationRepository: ClubApplicationRepository,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val clubStaffRepository: ClubStaffRepository
) {

    @Transactional
    fun createApplication(applicantUserId: Long, req: CreateClubApplicationRequest): ClubApplicationResponse {
        val applicant = userRepository.findById(applicantUserId)
            .orElseThrow { EntityNotFoundException("User not found") }

        // На старте: один PENDING на пользователя, чтобы не было мусора
        if (clubApplicationRepository.existsByApplicant_IdAndStatus(applicantUserId, ClubApplicationStatus.PENDING)) {
            throw IllegalStateException("You already have a pending club application")
        }

        val entity = ClubApplicationEntity(
            applicant = applicant,
            clubName = req.clubName.trim(),
            address = req.address.trim(),
            locationText = req.locationText?.trim(),
            description = req.description?.trim(),
            status = ClubApplicationStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val saved = clubApplicationRepository.save(entity)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun listMyApplications(applicantUserId: Long): List<ClubApplicationResponse> =
        clubApplicationRepository.findAllByApplicant_IdOrderByCreatedAtDesc(applicantUserId).map { it.toResponse() }

    // --- GLOBAL ADMIN ---

    @Transactional(readOnly = true)
    fun adminList(status: ClubApplicationStatus?): List<ClubApplicationResponse> =
        clubApplicationRepository.findAllFiltered(status).map { it.toResponse() }

    @Transactional
    fun approve(applicationId: Long, decidedByUserId: Long, req: ClubApplicationDecisionRequest): ApproveClubApplicationResponse {
        val app = clubApplicationRepository.findById(applicationId)
            .orElseThrow { EntityNotFoundException("Club application not found") }
        if (app.status != ClubApplicationStatus.PENDING) {
            throw IllegalStateException("Application is not in PENDING status")
        }

        val decidedBy = userRepository.findById(decidedByUserId)
            .orElseThrow { EntityNotFoundException("Decider user not found") }

        val ownerUserId = req.ownerUserId ?: (app.applicant.id ?: throw IllegalStateException("Applicant id is null"))
        val owner = userRepository.findById(ownerUserId)
            .orElseThrow { EntityNotFoundException("Owner user not found") }

        val club = ClubEntity(
            name = app.clubName,
            address = app.address,
            locationText = app.locationText,
            description = app.description,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val savedClub = clubRepository.save(club)

        // Назначаем OWNER в рамках созданного клуба
        clubStaffRepository.save(
            ClubStaffEntity(
                id = ClubStaffId(clubId = savedClub.id!!, userId = owner.id!!),
                club = savedClub,
                user = owner,
                role = ClubRole.OWNER,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        app.status = ClubApplicationStatus.APPROVED
        app.decisionComment = req.comment?.trim()
        app.decidedBy = decidedBy
        app.decidedAt = LocalDateTime.now()
        app.createdClub = savedClub
        app.updatedAt = LocalDateTime.now()
        clubApplicationRepository.save(app)

        return ApproveClubApplicationResponse(
            applicationId = app.id!!,
            createdClubId = savedClub.id!!,
            ownerUserId = owner.id!!
        )
    }

    @Transactional
    fun reject(applicationId: Long, decidedByUserId: Long, req: ClubApplicationDecisionRequest): ClubApplicationResponse {
        val app = clubApplicationRepository.findById(applicationId)
            .orElseThrow { EntityNotFoundException("Club application not found") }
        if (app.status != ClubApplicationStatus.PENDING) {
            throw IllegalStateException("Application is not in PENDING status")
        }

        val decidedBy = userRepository.findById(decidedByUserId)
            .orElseThrow { EntityNotFoundException("Decider user not found") }

        app.status = ClubApplicationStatus.REJECTED
        app.decisionComment = req.comment?.trim()
        app.decidedBy = decidedBy
        app.decidedAt = LocalDateTime.now()
        app.updatedAt = LocalDateTime.now()

        return clubApplicationRepository.save(app).toResponse()
    }

    private fun ClubApplicationEntity.toResponse(): ClubApplicationResponse = ClubApplicationResponse(
        id = this.id!!,
        applicantUserId = this.applicant.id!!,
        clubName = this.clubName,
        address = this.address,
        locationText = this.locationText,
        description = this.description,
        status = this.status,
        decisionComment = this.decisionComment,
        decidedByUserId = this.decidedBy?.id,
        decidedAt = this.decidedAt,
        createdClubId = this.createdClub?.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
