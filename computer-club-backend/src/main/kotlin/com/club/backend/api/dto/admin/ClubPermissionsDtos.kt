package com.club.backend.api.dto.admin

import com.club.backend.domain.entity.ClubRole
import com.club.backend.domain.enum.ClubPermission

data class SetPermissionOverrideRequest(
    val granted: Boolean
)

data class PermissionOverrideView(
    val permission: ClubPermission,
    val granted: Boolean
)

data class ClubStaffPermissionsResponse(
    val clubId: Long,
    val userId: Long,
    val role: ClubRole?,
    val rolePermissions: List<ClubPermission>,
    val overrides: List<PermissionOverrideView>,
    val effectivePermissions: List<ClubPermission>
)
