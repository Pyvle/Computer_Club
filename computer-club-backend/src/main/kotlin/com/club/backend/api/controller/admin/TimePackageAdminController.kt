package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AdminTimePackageResponse
import com.club.backend.api.dto.admin.CreateTimePackageRequest
import com.club.backend.api.dto.admin.UpdateTimePackageRequest
import com.club.backend.service.TimePackageAdminService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/time-packages")
class TimePackageAdminController(
    private val service: TimePackageAdminService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun list(@PathVariable clubId: Long): List<AdminTimePackageResponse> =
        service.list(clubId)

    @PostMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun create(
        @PathVariable clubId: Long,
        @RequestBody req: CreateTimePackageRequest
    ): AdminTimePackageResponse = service.create(currentUserId(), clubId, req)

    @PutMapping("/{id}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun update(
        @PathVariable clubId: Long,
        @PathVariable id: Long,
        @RequestBody req: UpdateTimePackageRequest
    ): AdminTimePackageResponse = service.update(currentUserId(), clubId, id, req)

    @DeleteMapping("/{id}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun delete(@PathVariable clubId: Long, @PathVariable id: Long) {
        service.delete(currentUserId(), clubId, id)
    }
}
