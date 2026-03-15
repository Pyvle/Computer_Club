package com.club.backend.api.dto

data class TimePackageResponse(
    val id: Long,
    val name: String,
    val hours: Int,
    val pricePerHourRub: Int,
    val totalPriceRub: Int,
    /** "HH:mm" или null, если пакет доступен в любое время. */
    val availableFrom: String?,
    val availableTo: String?
)
