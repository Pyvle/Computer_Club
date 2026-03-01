package com.club.backend.api.dto.admin

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AdminCategoryResponse(
    val id: Long,
    val title: String,
    val sortOrder: Int,
    val isActive: Boolean
)

data class AdminProductResponse(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String?,
    val isActive: Boolean
)

data class AdminClubCatalogProductResponse(
    val productId: Long,
    val categoryId: Long,
    val productTitle: String,
    val description: String?,
    val productIsActive: Boolean,
    val isLinkedToClub: Boolean,
    val clubPriceRub: Int?,
    val clubIsAvailable: Boolean?
)

data class AdminClubCatalogResponse(
    val categories: List<AdminCategoryResponse>,
    val products: List<AdminClubCatalogProductResponse>
)

data class CreateCategoryRequest(
    @field:NotBlank
    val title: String,
    val sortOrder: Int = 0
)

data class UpdateCategoryRequest(
    @field:NotBlank
    val title: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class CreateProductRequest(
    @field:NotNull
    val categoryId: Long,
    @field:NotBlank
    val title: String,
    val description: String? = null,
    val isActive: Boolean = true
)

data class UpdateProductRequest(
    @field:NotNull
    val categoryId: Long,
    @field:NotBlank
    val title: String,
    val description: String? = null,
    val isActive: Boolean = true
)

data class UpsertClubProductRequest(
    @field:NotNull
    @field:Min(0)
    val priceRub: Int,
    val isAvailable: Boolean = true
)
