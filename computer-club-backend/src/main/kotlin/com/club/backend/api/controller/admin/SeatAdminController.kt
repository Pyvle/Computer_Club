package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AdminSeatResponse
import com.club.backend.api.dto.admin.CreateSeatRequest
import com.club.backend.api.dto.admin.UpdateSeatRequest
import com.club.backend.service.SeatAdminService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/seats")
class SeatAdminController(
    private val seatAdminService: SeatAdminService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_SEATS_MANAGE)")
    fun list(@PathVariable clubId: Long): List<AdminSeatResponse> =
        seatAdminService.list(clubId)

    @PostMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_SEATS_MANAGE)")
    fun create(@PathVariable clubId: Long, @RequestBody req: CreateSeatRequest): AdminSeatResponse =
        seatAdminService.create(currentUserId(), clubId, req)

    @PutMapping("/{seatId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_SEATS_MANAGE)")
    fun update(
        @PathVariable clubId: Long,
        @PathVariable seatId: Long,
        @RequestBody req: UpdateSeatRequest
    ): AdminSeatResponse = seatAdminService.update(currentUserId(), clubId, seatId, req)

    @DeleteMapping("/{seatId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_SEATS_MANAGE)")
    fun delete(@PathVariable clubId: Long, @PathVariable seatId: Long) {
        seatAdminService.delete(currentUserId(), clubId, seatId)
    }
}
