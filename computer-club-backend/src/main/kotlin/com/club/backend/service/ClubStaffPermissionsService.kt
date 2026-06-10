package com.club.backend.service

import com.club.backend.api.dto.admin.ClubStaffPermissionsResponse
import com.club.backend.api.dto.admin.PermissionOverrideView
import com.club.backend.domain.entity.ClubPermissionRuleEntity
import com.club.backend.domain.entity.ClubPermissionRuleType
import com.club.backend.domain.enum.ClubPermission
import com.club.backend.repository.ClubPermissionRuleRepository
import com.club.backend.repository.ClubStaffRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime

@Service
class ClubStaffPermissionsService(
    private val clubStaffRepository: ClubStaffRepository,
    private val clubPermissionRuleRepository: ClubPermissionRuleRepository,
    private val auditService: AuditService
) {

    @Transactional(readOnly = true)
    fun get(clubId: Long, userId: Long): ClubStaffPermissionsResponse {
        val staff = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId).orElse(null)
        val role = staff?.role

        val rolePermissions = if (role == null) emptyList() else
            clubPermissionRuleRepository
                .findAllByRuleTypeAndRoleAndGrantedTrue(ClubPermissionRuleType.ROLE_DEFAULT, role)
                .map { it.permission }
                .distinct()
                .sortedBy { it.name }

        val overrides = clubPermissionRuleRepository.findOverridesByClubIdAndUserId(clubId, userId)
        val overrideViews = overrides
            .map { PermissionOverrideView(it.permission, it.granted) }
            .sortedBy { it.permission.name }

        val effective = rolePermissions.toMutableSet()
        overrides.forEach {
            if (it.granted) effective.add(it.permission) else effective.remove(it.permission)
        }

        return ClubStaffPermissionsResponse(
            clubId = clubId,
            userId = userId,
            role = role,
            rolePermissions = rolePermissions,
            overrides = overrideViews,
            effectivePermissions = effective.toList().sortedBy { it.name }
        )
    }

    @Transactional
    fun setOverride(actorUserId: Long, clubId: Long, userId: Long, permission: ClubPermission, granted: Boolean) {
        // пермишены назначаем только участникам стаффа — иначе модель теряет гарантии
        val staff = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "User $userId is not a staff member of club $clubId") }

        val existing = clubPermissionRuleRepository.findOverride(clubId, userId, permission).orElse(null)
        val now = OffsetDateTime.now()

        if (existing == null) {
            clubPermissionRuleRepository.save(
                ClubPermissionRuleEntity(
                    ruleType = ClubPermissionRuleType.USER_OVERRIDE,
                    club = staff.club,
                    user = staff.user,
                    permission = permission,
                    granted = granted,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val before = mapOf(
                "clubId" to clubId,
                "userId" to userId,
                "permission" to permission.name,
                "granted" to existing.granted
            )
            existing.granted = granted
            existing.updatedAt = now
            clubPermissionRuleRepository.save(existing)

            val after = mapOf(
                "clubId" to clubId,
                "userId" to userId,
                "permission" to permission.name,
                "granted" to granted
            )
            auditService.log(
                actorUserId = actorUserId,
                clubId = clubId,
                action = "PERMISSION_OVERRIDE_SET",
                entityType = "PermissionOverride",
                entityId = "$clubId:$userId:${permission.name}",
                before = before,
                after = after
            )
        }

        if (existing == null) {
            val after = mapOf(
                "clubId" to clubId,
                "userId" to userId,
                "permission" to permission.name,
                "granted" to granted
            )
            auditService.log(
                actorUserId = actorUserId,
                clubId = clubId,
                action = "PERMISSION_OVERRIDE_SET",
                entityType = "PermissionOverride",
                entityId = "$clubId:$userId:${permission.name}",
                before = null,
                after = after
            )
        }
    }

    @Transactional
    fun deleteOverride(actorUserId: Long, clubId: Long, userId: Long, permission: ClubPermission) {
        val existing = clubPermissionRuleRepository.findOverride(clubId, userId, permission).orElse(null)
        if (existing != null) {
            val before = mapOf(
                "clubId" to clubId,
                "userId" to userId,
                "permission" to permission.name,
                "granted" to existing.granted
            )
            clubPermissionRuleRepository.delete(existing)
            auditService.log(
                actorUserId = actorUserId,
                clubId = clubId,
                action = "PERMISSION_OVERRIDE_DELETE",
                entityType = "PermissionOverride",
                entityId = "$clubId:$userId:${permission.name}",
                before = before,
                after = null
            )
        }
    }
}
