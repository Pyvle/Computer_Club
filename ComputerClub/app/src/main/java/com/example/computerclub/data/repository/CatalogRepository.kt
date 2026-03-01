package com.example.computerclub.data.repository

import com.example.computerclub.data.network.ProductApi
import com.example.computerclub.data.network.dto.ClubProductResponseDto
import com.example.computerclub.data.network.dto.ProductCategoryResponseDto

class CatalogRepository(private val api: ProductApi) {
    suspend fun categories(): List<ProductCategoryResponseDto> = api.getCategories()
    suspend fun clubProducts(clubId: Long): List<ClubProductResponseDto> = api.getClubProducts(clubId)
}
