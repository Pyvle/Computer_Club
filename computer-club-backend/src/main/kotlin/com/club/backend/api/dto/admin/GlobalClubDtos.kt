package com.club.backend.api.dto.admin

import java.time.Instant

data class GlobalClubResponse(
    val id: Long,
    val name: String,
    val addressShort: String,
    val addressFull: String,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean,
    val isBlocked: Boolean,
    val blockReason: String?,
    val createdAt: String
)

data class BlockClubRequest(
    val reason: String?
)

data class ClubWarningRequest(
    val message: String
)

data class ClubWarningResponse(
    val id: Long,
    val message: String,
    val createdBy: Long,
    val createdAt: Instant
)
