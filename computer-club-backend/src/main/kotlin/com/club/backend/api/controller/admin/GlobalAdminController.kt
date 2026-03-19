package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.AdminUserResponse
import com.club.backend.api.dto.admin.BlockClubRequest
import com.club.backend.api.dto.admin.ClubWarningRequest
import com.club.backend.api.dto.admin.ClubWarningResponse
import com.club.backend.api.dto.admin.CreateUserRequest
import com.club.backend.api.dto.admin.GlobalClubResponse
import com.club.backend.api.dto.admin.SetActiveRequest
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
