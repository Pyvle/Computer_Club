package com.club.backend.api.dto

import com.club.backend.domain.enum.ClubReportStatus
import java.time.Instant

data class CreateReportRequest(
    val message: String
)

data class ClubUserReportResponse(
    val id: Long,
    val userId: Long,
    val userPhone: String?,
    val message: String,
    val status: ClubReportStatus,
    val createdAt: Instant
)

data class UpdateReportStatusRequest(
    val status: ClubReportStatus
)

/** Предупреждение от GLOBAL_ADMIN, видимое владельцу клуба. */
data class PlatformMessageResponse(
    val id: Long,
    val message: String,
    val createdAt: Instant
)
