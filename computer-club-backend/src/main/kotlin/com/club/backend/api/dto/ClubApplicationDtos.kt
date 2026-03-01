package com.club.backend.api.dto

import com.club.backend.domain.entity.ClubApplicationStatus
import java.time.LocalDateTime

data class CreateClubApplicationRequest(
    val clubName: String,
    val address: String,
    val locationText: String? = null,
    val description: String? = null
)

data class ClubApplicationResponse(
    val id: Long,
    val applicantUserId: Long,
    val clubName: String,
    val address: String,
    val locationText: String?,
    val description: String?,
    val status: ClubApplicationStatus,
    val decisionComment: String?,
    val decidedByUserId: Long?,
    val decidedAt: LocalDateTime?,
    val createdClubId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ClubApplicationDecisionRequest(
    val comment: String? = null,
    /** Если не задано при approve — ownerом становится заявитель */
    val ownerUserId: Long? = null
)

data class ApproveClubApplicationResponse(
    val applicationId: Long,
    val createdClubId: Long,
    val ownerUserId: Long
)
