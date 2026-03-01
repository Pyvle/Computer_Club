package com.club.backend.api.controller.admin

import com.club.backend.api.dto.*
import com.club.backend.service.FloorplanAdminService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/floorplans")
class ClubFloorplanAdminController(
    private val floorplanAdminService: FloorplanAdminService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun list(@PathVariable clubId: Long): List<FloorplanSummaryResponse> =
        floorplanAdminService.list(clubId)

    @PostMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun create(@PathVariable clubId: Long, @RequestBody req: CreateFloorplanRequest): FloorplanResponse =
        floorplanAdminService.create(currentUserId(), clubId, req)

    @GetMapping("/{floorplanId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun get(@PathVariable clubId: Long, @PathVariable floorplanId: Long): FloorplanResponse =
        floorplanAdminService.get(clubId, floorplanId)

    @PutMapping("/{floorplanId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun update(
        @PathVariable clubId: Long,
        @PathVariable floorplanId: Long,
        @RequestBody req: UpdateFloorplanRequest
    ): FloorplanResponse = floorplanAdminService.update(currentUserId(), clubId, floorplanId, req)

    @PostMapping("/{floorplanId}/publish")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun publish(@PathVariable clubId: Long, @PathVariable floorplanId: Long): PublishFloorplanResponse =
        floorplanAdminService.publish(currentUserId(), clubId, floorplanId)

    @PostMapping("/{floorplanId}/unpublish")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun unpublish(@PathVariable clubId: Long, @PathVariable floorplanId: Long) {
        floorplanAdminService.unpublish(currentUserId(), clubId, floorplanId)
    }

    @PostMapping("/{floorplanId}/clone")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun clone(
        @PathVariable clubId: Long,
        @PathVariable floorplanId: Long,
        @RequestBody req: CloneFloorplanRequest
    ): FloorplanResponse = floorplanAdminService.clone(currentUserId(), clubId, floorplanId, req)

    @DeleteMapping("/{floorplanId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_FLOORPLANS_MANAGE)")
    fun archive(@PathVariable clubId: Long, @PathVariable floorplanId: Long) {
        floorplanAdminService.archive(currentUserId(), clubId, floorplanId)
    }
}
