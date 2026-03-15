package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AdminBookingDetailResponse
import com.club.backend.api.dto.admin.AdminBookingResponse
import com.club.backend.api.dto.admin.AdminPurchaseDetailResponse
import com.club.backend.api.dto.admin.AdminPurchaseResponse
import com.club.backend.api.dto.admin.ClubDashboardResponse
import com.club.backend.api.dto.admin.FloorplanBookingEntry
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.security.RbacService
import com.club.backend.service.ClubReportsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}")
class ClubReportsAdminController(
    private val clubReportsService: ClubReportsService,
    private val rbacService: RbacService
) {

    @GetMapping("/dashboard")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun dashboard(@PathVariable clubId: Long, authentication: Authentication): ClubDashboardResponse =
        clubReportsService.dashboard(clubId, includeExtendedRevenue = rbacService.isOwner(authentication, clubId))

    @GetMapping("/floorplan-bookings")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun floorplanBookings(
        @PathVariable clubId: Long,
        @RequestParam at: String
    ): List<FloorplanBookingEntry> =
        clubReportsService.floorplanBookings(clubId, LocalDateTime.parse(at))

    @GetMapping("/bookings")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun bookings(
        @PathVariable clubId: Long,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) status: BookingStatus?
    ): List<AdminBookingResponse> =
        clubReportsService.bookings(
            clubId = clubId,
            from = from?.let(LocalDateTime::parse),
            to = to?.let(LocalDateTime::parse),
            status = status
        )

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun bookingDetail(
        @PathVariable clubId: Long,
        @PathVariable bookingId: Long
    ): AdminBookingDetailResponse = clubReportsService.bookingDetail(clubId, bookingId)

    @GetMapping("/purchases")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun purchases(
        @PathVariable clubId: Long,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) status: PaymentStatus?
    ): List<AdminPurchaseResponse> =
        clubReportsService.purchases(
            clubId = clubId,
            from = from?.let(LocalDateTime::parse),
            to = to?.let(LocalDateTime::parse),
            status = status
        )

    @GetMapping("/purchases/{purchaseId}")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun purchaseDetail(
        @PathVariable clubId: Long,
        @PathVariable purchaseId: Long
    ): AdminPurchaseDetailResponse = clubReportsService.purchaseDetail(clubId, purchaseId)

    @PostMapping("/purchases/{purchaseId}/cancel")
    @PreAuthorize("@rbac.hasClubPermission(authentication, #clubId, T(com.club.backend.domain.enum.ClubPermission).CLUB_REPORTS_VIEW)")
    fun cancelPurchase(
        @PathVariable clubId: Long,
        @PathVariable purchaseId: Long
    ): AdminPurchaseResponse = clubReportsService.adminCancel(clubId, purchaseId)
}
