package com.club.backend.domain.entity

import com.club.backend.domain.enum.ClubPermission
import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class ClubRolePermissionId(
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    var role: ClubRole = ClubRole.ADMIN,

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 64)
    var permission: ClubPermission = ClubPermission.CLUB_CATALOG_MANAGE
) : Serializable

@Entity
@Table(name = "club_role_permissions")
class ClubRolePermissionEntity(
    @EmbeddedId
    var id: ClubRolePermissionId
)
