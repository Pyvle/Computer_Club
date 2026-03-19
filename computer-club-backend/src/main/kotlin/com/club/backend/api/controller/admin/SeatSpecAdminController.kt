package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.SeatSpecResponse
import com.club.backend.api.dto.admin.UpdateSeatSpecRequest
import com.club.backend.service.ClubSeatSpecService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/seat-specs")
class SeatSpecAdminController(
    private val seatSpecService: ClubSeatSpecService
) {

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_SEATS_MANAGE)")
    fun list(@PathVariable clubId: Long): List<SeatSpecResponse> =
        seatSpecService.getByClub(clubId)

    @PutMapping("/{seatType}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_SEATS_MANAGE)")
    fun update(
        @PathVariable clubId: Long,
        @PathVariable seatType: String,
        @RequestBody req: UpdateSeatSpecRequest
    ): SeatSpecResponse = seatSpecService.update(clubId, seatType, req)
}
