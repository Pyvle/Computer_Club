package com.club.backend.api.dto

import java.time.LocalDateTime

data class AvailableClubResponse(
    val id: Long,
    val name: String,
    val address: String,
    val locationText: String?,
    val description: String?,
    val imageUrl: String?,
    val isBlocked: Boolean,
    val blockReason: String?,
    val blockedUntil: LocalDateTime?,
    val latitude: Double?,
    val longitude: Double?
)
