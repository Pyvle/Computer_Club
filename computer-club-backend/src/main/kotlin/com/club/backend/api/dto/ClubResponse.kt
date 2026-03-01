package com.club.backend.api.dto

data class ClubResponse(
    val id: Long,
    val name: String,
    val address: String,
    val locationText: String?,
    val description: String?
)