package com.club.backend.api.dto

data class ClubProductResponse(
    val productId: Long,
    val categoryId: Long,
    val categoryTitle: String,
    val title: String,
    val description: String?,
    val priceRub: Int,
    val isAvailable: Boolean
)