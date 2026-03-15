package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TimePackageResponseDto(
    val id: Long,
    val name: String,
    val hours: Int,
    val pricePerHourRub: Int,
    val totalPriceRub: Int,
    /** "HH:mm" или null — доступен в любое время суток. */
    val availableFrom: String? = null,
    val availableTo: String? = null
)
