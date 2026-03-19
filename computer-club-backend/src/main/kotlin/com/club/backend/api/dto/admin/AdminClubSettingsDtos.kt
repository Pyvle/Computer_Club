package com.club.backend.api.dto.admin

data class ClubSettingsResponse(
    val id: Long,
    val name: String,
    val addressFull: String,
    val addressShort: String,
    val locationText: String?,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

data class UpdateClubSettingsRequest(
    val name: String,
    val addressFull: String,
    val addressShort: String,
    val locationText: String?,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean,
    val latitude: Double?,
    val longitude: Double?
)
