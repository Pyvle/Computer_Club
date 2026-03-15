package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.ClubSettingsResponse
import com.club.backend.api.dto.admin.UpdateClubSettingsRequest
import com.club.backend.service.ClubSettingsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/settings")
class ClubSettingsController(
    private val clubSettingsService: ClubSettingsService
) {

    @GetMapping
    @PreAuthorize("@rbac.isOwner(authentication, #clubId)")
    fun get(@PathVariable clubId: Long): ClubSettingsResponse =
        clubSettingsService.get(clubId)

    @PutMapping
    @PreAuthorize("@rbac.isOwner(authentication, #clubId)")
    fun update(
        @PathVariable clubId: Long,
        @RequestBody req: UpdateClubSettingsRequest,
        authentication: Authentication
    ): ClubSettingsResponse = clubSettingsService.update(clubId, req, authentication.name.toLong())
}
