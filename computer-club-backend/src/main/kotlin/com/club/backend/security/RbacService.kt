package com.club.backend.security

import com.club.backend.domain.entity.ClubRole
import com.club.backend.domain.enum.ClubPermission
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.ClubRolePermissionRepository
import com.club.backend.repository.ClubUserPermissionOverrideRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Один слой авторизации, чтобы потом легко расширяться (новые роли/права)
 * без переписывания контроллеров.
 */
@Component("rbac")
class RbacService(
    private val clubStaffRepository: ClubStaffRepository,
    private val clubRolePermissionRepository: ClubRolePermissionRepository,
    private val clubUserPermissionOverrideRepository: ClubUserPermissionOverrideRepository
) {

    private fun userId(auth: Authentication): Long = auth.principal.toString().toLong()

    fun isGlobalAdmin(auth: Authentication): Boolean =
        auth.authorities.any { it.authority == "ROLE_GLOBAL_ADMIN" }

    /**
     * Проверяет клубный пермишен для пользователя.
     *
     * Порядок проверки:
     * 1) GLOBAL_ADMIN → всегда true
     * 2) персональный override (clubId, userId, permission) → granted/denied
     * 3) маппинг роли на пермишены (club_role_permissions)
     */
    fun hasClubPermission(auth: Authentication, clubId: Long, permission: ClubPermission): Boolean {
        if (isGlobalAdmin(auth)) return true
        val uid = userId(auth)

        // пермишены клуба применимы только к участникам стаффа
        val role = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, uid).orElse(null)?.role
            ?: return false

        val override = clubUserPermissionOverrideRepository
            .findByIdClubIdAndIdUserIdAndIdPermission(clubId, uid, permission)
            .orElse(null)
        if (override != null) return override.granted

        return clubRolePermissionRepository.existsByIdRoleAndIdPermission(role, permission)
    }

    fun canManageClub(auth: Authentication, clubId: Long): Boolean {
        if (isGlobalAdmin(auth)) return true
        val uid = userId(auth)
        return clubStaffRepository.existsWithAnyRole(clubId, uid, listOf(ClubRole.OWNER, ClubRole.ADMIN))
    }

    /** Возвращает true если пользователь является OWNER клуба (или GLOBAL_ADMIN). */
    fun isOwner(auth: Authentication, clubId: Long): Boolean {
        if (isGlobalAdmin(auth)) return true
        val uid = userId(auth)
        return clubStaffRepository.existsWithAnyRole(clubId, uid, listOf(ClubRole.OWNER))
    }

    fun canManageClubAdmins(auth: Authentication, clubId: Long): Boolean {
        return hasClubPermission(auth, clubId, ClubPermission.CLUB_ADMINS_MANAGE)
    }
}
