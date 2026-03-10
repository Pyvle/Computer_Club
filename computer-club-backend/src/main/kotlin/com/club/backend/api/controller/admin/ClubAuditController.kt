package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AuditLogResponse
import com.club.backend.service.AuditQueryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/audit")
class ClubAuditController(
    private val auditQueryService: AuditQueryService
) {
    @GetMapping
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun list(
        @PathVariable clubId: Long,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(required = false, defaultValue = "200") limit: Int
    ): List<AuditLogResponse> = auditQueryService.listForClub(clubId, action, from, to, limit)
}
