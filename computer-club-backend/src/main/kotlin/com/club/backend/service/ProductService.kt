package com.club.backend.service

import com.club.backend.api.dto.ClubProductResponse
import com.club.backend.api.dto.ProductCategoryResponse
import com.club.backend.repository.ClubProductRepository
import com.club.backend.repository.ProductCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productCategoryRepository: ProductCategoryRepository,
    private val clubProductRepository: ClubProductRepository,
    private val clubAccessService: ClubAccessService
) {

    @Transactional(readOnly = true)
    fun getActiveCategories(): List<ProductCategoryResponse> {
        return productCategoryRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc().map {
            ProductCategoryResponse(
                id = it.id!!,
                title = it.title,
                sortOrder = it.sortOrder
            )
        }
    }

    @Transactional(readOnly = true)
    fun getClubMenu(userId: Long?, clubId: Long): List<ClubProductResponse> {
        // анонимный пользователь видит меню без проверки блокировки
        if (userId != null) clubAccessService.ensureNotBlocked(userId, clubId)
        return clubProductRepository.findMenuByClubId(clubId).map { cp ->
            ClubProductResponse(
                productId = cp.product.id!!,
                categoryId = cp.product.category.id!!,
                categoryTitle = cp.product.category.title,
                title = cp.product.title,
                description = cp.product.description,
                imageUrl = cp.product.imageUrl,
                priceRub = cp.priceRub,
                isAvailable = cp.isAvailable
            )
        }
    }
}