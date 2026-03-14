package com.club.backend.api.controller.admin

import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ProductRepository
import com.club.backend.service.FileStorageService
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

data class ImageUrlResponse(val imageUrl: String)

@RestController
class ImageUploadController(
    private val fileStorageService: FileStorageService,
    private val clubRepository: ClubRepository,
    private val productRepository: ProductRepository
) {

    @PostMapping("/api/v1/admin/clubs/{clubId}/image")
    @PreAuthorize("@rbac.canManageClub(authentication, #clubId)")
    fun uploadClubImage(
        @PathVariable clubId: Long,
        @RequestParam("file") file: MultipartFile
    ): ImageUrlResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { EntityNotFoundException("Club $clubId not found") }
        val url = fileStorageService.uploadClubImage(clubId, file)
        club.imageUrl = url
        club.updatedAt = LocalDateTime.now()
        clubRepository.save(club)
        return ImageUrlResponse(url)
    }

    @PostMapping("/api/v1/admin/global/catalog/products/{productId}/image")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    fun uploadProductImage(
        @PathVariable productId: Long,
        @RequestParam("file") file: MultipartFile
    ): ImageUrlResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { EntityNotFoundException("Product $productId not found") }
        val url = fileStorageService.uploadProductImage(productId, file)
        product.imageUrl = url
        productRepository.save(product)
        return ImageUrlResponse(url)
    }
}
