package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductCategoryResponseDto(
    val id: Long,
    val title: String,
    val sortOrder: Int
)

@Serializable
data class ClubProductResponseDto(
    val productId: Long,
    val categoryId: Long,
    val categoryTitle: String,
    val title: String,
    val description: String? = null,
    val priceRub: Int,
    val isAvailable: Boolean
)
