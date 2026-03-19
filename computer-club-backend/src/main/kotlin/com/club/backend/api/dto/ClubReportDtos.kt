package com.club.backend.api.dto

import java.time.Instant

data class CreateReportRequest(
    val message: String
)

data class ClubUserReportResponse(
    val id: Long,
    val userId: Long,
    val userPhone: String?,
    val message: String,
    val createdAt: Instant
)

/** Предупреждение от GLOBAL_ADMIN, видимое владельцу клуба. */
data class PlatformMessageResponse(
    val id: Long,
    val message: String,
    val createdAt: Instant
)
