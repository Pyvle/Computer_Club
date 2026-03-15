package com.club.backend.api.dto.admin

data class AdminTimePackageResponse(
    val id: Long,
    val name: String,
    val hours: Int,
    val pricePerHourRub: Int,
    val totalPriceRub: Int,
    val isActive: Boolean,
    val sortOrder: Int,
    /** "HH:mm" или null, если пакет доступен в любое время. */
    val availableFrom: String?,
    val availableTo: String?
)

data class CreateTimePackageRequest(
    val name: String,
    val hours: Int,
    val pricePerHourRub: Int,
    val sortOrder: Int = 0,
    /** "HH:mm" или null. */
    val availableFrom: String? = null,
    val availableTo: String? = null
)

data class UpdateTimePackageRequest(
    val name: String,
    val hours: Int,
    val pricePerHourRub: Int,
    val isActive: Boolean,
    val sortOrder: Int,
    val availableFrom: String? = null,
    val availableTo: String? = null
)
