package com.club.backend.api.dto.admin

data class ClubSettingsResponse(
    val id: Long,
    val name: String,
    val address: String,
    val locationText: String?,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean
)

data class UpdateClubSettingsRequest(
    val name: String,
    val address: String,
    val locationText: String?,
    val description: String?,
    val isActive: Boolean
)
