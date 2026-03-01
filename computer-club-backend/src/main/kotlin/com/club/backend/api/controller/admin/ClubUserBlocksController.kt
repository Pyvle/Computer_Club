package com.club.backend.api.controller.admin

import com.club.backend.service.ClubUserBlockAdminService
import com.club.backend.service.ClubUserBlockView
import com.club.backend.service.UpsertClubUserBlockRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/user-blocks")
class ClubUserBlocksController(
    private val clubUserBlockAdminService: ClubUserBlockAdminService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_USER_BLOCKS_MANAGE)")
    fun list(@PathVariable clubId: Long): List<ClubUserBlockView> =
        clubUserBlockAdminService.list(clubId)

    @PutMapping("/{userId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_USER_BLOCKS_MANAGE)")
    fun upsert(
        @PathVariable clubId: Long,
        @PathVariable userId: Long,
        @RequestBody req: UpsertClubUserBlockRequest
    ): ClubUserBlockView =
        clubUserBlockAdminService.upsert(currentUserId(), clubId, userId, req)
}
