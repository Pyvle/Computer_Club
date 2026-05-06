package com.club.backend.api.controller.admin

import com.club.backend.api.dto.UploadResponse
import com.club.backend.service.FileStorageService
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/admin/uploads")
class UploadController(private val fileStorageService: FileStorageService) {

    @PostMapping("/club-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun uploadClubImage(@RequestPart("file") file: MultipartFile): UploadResponse {
        val path = fileStorageService.saveClubImage(file)
        return UploadResponse(path)
    }

    @PostMapping("/product-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun uploadProductImage(@RequestPart("file") file: MultipartFile): UploadResponse {
        val path = fileStorageService.saveProductImage(file)
        return UploadResponse(path)
    }
}
