package com.club.backend.api.controller.admin

import com.club.backend.service.ClubAdminManagementService
import com.club.backend.service.ClubStaffView
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/admins")
class ClubAdminsController(
    private val clubAdminManagementService: ClubAdminManagementService
) {

    @GetMapping
    @PreAuthorize("@rbac.canManageClubAdmins(authentication, #clubId)")
    fun list(@PathVariable clubId: Long): List<ClubStaffView> =
        clubAdminManagementService.listStaff(clubId)

    @PutMapping("/{userId}")
    @PreAuthorize("@rbac.canManageClubAdmins(authentication, #clubId)")
    fun add(@PathVariable clubId: Long, @PathVariable userId: Long): ClubStaffView =
        clubAdminManagementService.upsertAdmin(clubId, userId)

    @DeleteMapping("/{userId}")
    @PreAuthorize("@rbac.canManageClubAdmins(authentication, #clubId)")
    fun remove(@PathVariable clubId: Long, @PathVariable userId: Long) {
        clubAdminManagementService.removeAdmin(clubId, userId)
    }

    @GetMapping("/users/by-phone")
    @PreAuthorize("@rbac.canManageClubAdmins(authentication, #clubId)")
    fun lookupByPhone(@PathVariable clubId: Long, @RequestParam phone: String): ClubStaffView =
        clubAdminManagementService.lookupByPhone(phone)
}
