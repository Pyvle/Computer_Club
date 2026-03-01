package com.club.backend.api.controller.admin

import com.club.backend.service.GlobalAdminService
import com.club.backend.service.SetGlobalRoleRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/global")
class GlobalAdminController(
    private val globalAdminService: GlobalAdminService
) {

    @PutMapping("/users/{userId}/global-role")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun setGlobalRole(@PathVariable userId: Long, @RequestBody req: SetGlobalRoleRequest) {
        globalAdminService.setGlobalRole(userId, req.role)
    }

    @PutMapping("/clubs/{clubId}/owner/{userId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun setOwner(@PathVariable clubId: Long, @PathVariable userId: Long) {
        globalAdminService.setClubOwner(clubId, userId)
    }
}
