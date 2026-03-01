package com.club.backend.api.dto

data class ProductCategoryResponse(
    val id: Long,
    val title: String,
    val sortOrder: Int
)