package com.club.backend.api.dto

data class ClubResponse(
    val id: Long,
    val name: String,
    val address: String,
    val locationText: String?,
    val description: String?,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    // минимальная цена из всех типов мест, null если тарифы ещё не заданы
    val minPricePerHourRub: Int?
)