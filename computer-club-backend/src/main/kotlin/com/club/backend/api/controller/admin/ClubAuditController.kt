package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AuditLogResponse
import com.club.backend.domain.enum.ClubPermission
import com.club.backend.service.AuditQueryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/audit")
class ClubAuditController(
    private val auditQueryService: AuditQueryService
) {
    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun latest(
        @PathVariable clubId: Long,
        @RequestParam(value = "limit", required = false, defaultValue = "50") limit: Int
    ): List<AuditLogResponse> = auditQueryService.latestForClub(clubId, limit)
}
