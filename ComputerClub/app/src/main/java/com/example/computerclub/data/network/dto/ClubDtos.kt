package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClubResponseDto(
    val id: Long,
    val name: String,
    val address: String,
    val locationText: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class AvailableClubResponseDto(
    val id: Long,
    val name: String,
    val address: String,
    val locationText: String? = null,
    val description: String? = null,
    val isBlocked: Boolean,
    val blockReason: String? = null,
    val blockedUntil: String? = null, // не используем сейчас
    val latitude: Double? = null,
    val longitude: Double? = null
)