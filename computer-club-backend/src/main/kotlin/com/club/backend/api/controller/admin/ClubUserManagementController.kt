package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.ClubUserDetailResponse
import com.club.backend.api.dto.admin.ClubUserListItem
import com.club.backend.service.ClubUserManagementService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/users")
class ClubUserManagementController(
    private val clubUserManagementService: ClubUserManagementService
) {

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun listClubUsers(@PathVariable clubId: Long): List<ClubUserListItem> =
        clubUserManagementService.listClubUsers(clubId)

    @GetMapping("/{userId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun getClubUserDetail(
        @PathVariable clubId: Long,
        @PathVariable userId: Long
    ): ClubUserDetailResponse = clubUserManagementService.getClubUserDetail(clubId, userId)
}
