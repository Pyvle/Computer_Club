package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AdminSeatPriceResponse
import com.club.backend.api.dto.admin.UpsertSeatPriceRequest
import com.club.backend.service.SeatPriceAdminService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/seat-prices")
class SeatPriceAdminController(
    private val service: SeatPriceAdminService
) {

    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun list(@PathVariable clubId: Long): List<AdminSeatPriceResponse> =
        service.list(clubId)

    @PutMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_CATALOG_MANAGE)")
    fun upsert(
        @PathVariable clubId: Long,
        @RequestBody req: UpsertSeatPriceRequest
    ): AdminSeatPriceResponse = service.upsert(clubId, req)
}
