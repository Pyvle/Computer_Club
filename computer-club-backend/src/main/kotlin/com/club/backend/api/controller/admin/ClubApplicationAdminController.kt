package com.club.backend.api.controller.admin

import com.club.backend.api.dto.*
import com.club.backend.domain.entity.ClubApplicationStatus
import com.club.backend.service.ClubApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/club-applications")
class ClubApplicationAdminController(
    private val clubApplicationService: ClubApplicationService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun list(@RequestParam(required = false) status: String?): List<ClubApplicationResponse> {
        val s = status?.let { runCatching { ClubApplicationStatus.valueOf(it) }.getOrNull() }
        if (status != null && s == null) throw IllegalArgumentException("Unknown status")
        return clubApplicationService.adminList(s)
    }

    @PostMapping("/{applicationId}/approve")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun approve(
        @PathVariable applicationId: Long,
        @RequestBody req: ClubApplicationDecisionRequest
    ): ApproveClubApplicationResponse =
        clubApplicationService.approve(applicationId, currentUserId(), req)

    @PostMapping("/{applicationId}/reject")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun reject(
        @PathVariable applicationId: Long,
        @RequestBody req: ClubApplicationDecisionRequest
    ): ClubApplicationResponse =
        clubApplicationService.reject(applicationId, currentUserId(), req)
}
