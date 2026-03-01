package com.club.backend.api.controller

import com.club.backend.api.dto.ClubProductResponse
import com.club.backend.api.dto.ProductCategoryResponse
import com.club.backend.service.ProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/v1")
class ProductController(
    private val productService: ProductService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping("/product-categories")
    fun getCategories(): List<ProductCategoryResponse> =
        productService.getActiveCategories()

    @GetMapping("/clubs/{clubId}/products")
    fun getClubProducts(@PathVariable clubId: Long): List<ClubProductResponse> =
        productService.getClubMenu(currentUserId(), clubId)
}