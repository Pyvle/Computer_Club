package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.*
import com.club.backend.api.dto.admin.AdminPurchaseDetailResponse
import com.club.backend.service.GlobalAdminService
import com.club.backend.service.GlobalClubAdminService
import com.club.backend.service.SetGlobalRoleRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/global")
class GlobalAdminController(
    private val globalAdminService: GlobalAdminService,
    private val globalClubAdminService: GlobalClubAdminService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalStateException("Not authenticated")
        return principal.toLong()
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun listUsers(): List<AdminUserResponse> = globalAdminService.listUsers()

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getUserDetails(@PathVariable userId: Long): AdminUserDetailsResponse =
        globalAdminService.getUserDetails(userId)

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun createUser(@Valid @RequestBody req: CreateUserRequest): AdminUserResponse =
        globalAdminService.createUser(req)

    @PutMapping("/users/{userId}/active")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun toggleUserActive(@PathVariable userId: Long, @RequestBody req: SetActiveRequest) {
        globalAdminService.toggleUserActive(userId, req.isActive)
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun deleteUser(@PathVariable userId: Long) {
        globalAdminService.deleteUser(userId, currentUserId())
    }

    @GetMapping("/users/{userId}/bookings")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getUserBookings(@PathVariable userId: Long): List<GlobalAdminUserBookingItem> =
        globalAdminService.getUserAllBookings(userId)

    @GetMapping("/users/{userId}/purchases")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getUserPurchases(@PathVariable userId: Long): List<GlobalAdminUserPurchaseItem> =
        globalAdminService.getUserAllPurchases(userId)

    @GetMapping("/users/{userId}/reports")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getUserReports(@PathVariable userId: Long): List<GlobalAdminUserReportItem> =
        globalAdminService.getUserAllReports(userId)

    @GetMapping("/purchases/{purchaseId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getPurchaseDetail(@PathVariable purchaseId: Long): AdminPurchaseDetailResponse =
        globalAdminService.getPurchaseDetail(purchaseId)

    @PutMapping("/users/{userId}/global-role")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun setGlobalRole(@PathVariable userId: Long, @RequestBody req: SetGlobalRoleRequest) {
        globalAdminService.setGlobalRole(userId, req.role)
    }

    @PutMapping("/clubs/{clubId}/owner/{userId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun setOwner(@PathVariable clubId: Long, @PathVariable userId: Long) {
        globalAdminService.setClubOwner(clubId, userId)
    }

    // --- Управление клубами ---

    @GetMapping("/clubs")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun listClubs(): List<GlobalClubResponse> = globalClubAdminService.listAll()

    @GetMapping("/clubs/{clubId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getClubDetails(@PathVariable clubId: Long): GlobalClubDetailsResponse =
        globalClubAdminService.getDetails(clubId)

    @PutMapping("/clubs/{clubId}/block")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun blockClub(@PathVariable clubId: Long, @RequestBody req: BlockClubRequest) {
        globalClubAdminService.blockClub(clubId, req)
    }

    @PutMapping("/clubs/{clubId}/unblock")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun unblockClub(@PathVariable clubId: Long) {
        globalClubAdminService.unblockClub(clubId)
    }

    @DeleteMapping("/clubs/{clubId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun deleteClub(@PathVariable clubId: Long) {
        globalClubAdminService.deleteClub(clubId)
    }

    @PostMapping("/clubs/{clubId}/warnings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun addWarning(
        @PathVariable clubId: Long,
        @RequestBody req: ClubWarningRequest
    ): ClubWarningResponse = globalClubAdminService.addWarning(clubId, currentUserId(), req)

    @GetMapping("/clubs/{clubId}/warnings")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    fun getWarnings(@PathVariable clubId: Long): List<ClubWarningResponse> =
        globalClubAdminService.getWarnings(clubId)
}
