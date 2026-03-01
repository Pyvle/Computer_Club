package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.ClubStaffPermissionsResponse
import com.club.backend.api.dto.admin.SetPermissionOverrideRequest
import com.club.backend.domain.enum.ClubPermission
import com.club.backend.service.ClubStaffPermissionsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/staff/{userId}/permissions")
class ClubStaffPermissionsController(
    private val clubStaffPermissionsService: ClubStaffPermissionsService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_ADMINS_MANAGE)")
    fun get(
        @PathVariable clubId: Long,
        @PathVariable userId: Long
    ): ClubStaffPermissionsResponse = clubStaffPermissionsService.get(clubId, userId)

    @PutMapping("/{permission}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_ADMINS_MANAGE)")
    fun set(
        @PathVariable clubId: Long,
        @PathVariable userId: Long,
        @PathVariable permission: ClubPermission,
        @RequestBody req: SetPermissionOverrideRequest
    ) {
        clubStaffPermissionsService.setOverride(currentUserId(), clubId, userId, permission, req.granted)
    }

    @DeleteMapping("/{permission}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_ADMINS_MANAGE)")
    fun delete(
        @PathVariable clubId: Long,
        @PathVariable userId: Long,
        @PathVariable permission: ClubPermission
    ) {
        clubStaffPermissionsService.deleteOverride(currentUserId(), clubId, userId, permission)
    }
}
