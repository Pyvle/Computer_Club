package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AdminClubCatalogResponse
import com.club.backend.api.dto.admin.UpsertClubProductRequest
import com.club.backend.service.ClubCatalogAdminService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/catalog")
class ClubCatalogAdminController(
    private val clubCatalogAdminService: ClubCatalogAdminService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun get(@PathVariable clubId: Long): AdminClubCatalogResponse =
        clubCatalogAdminService.getClubCatalog(clubId)

    @PutMapping("/products/{productId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun upsertProduct(
        @PathVariable clubId: Long,
        @PathVariable productId: Long,
        @RequestBody req: UpsertClubProductRequest
    ) {
        clubCatalogAdminService.upsertClubProduct(currentUserId(), clubId, productId, req)
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unlinkProduct(
        @PathVariable clubId: Long,
        @PathVariable productId: Long
    ) {
        clubCatalogAdminService.unlinkClubProduct(currentUserId(), clubId, productId)
    }
}
