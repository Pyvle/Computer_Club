package com.club.backend.domain.entity

import com.club.backend.domain.enum.ClubPermission
import jakarta.persistence.*
import java.io.Serializable
import java.time.OffsetDateTime

@Embeddable
data class ClubUserPermissionOverrideId(
    @Column(name = "club_id", nullable = false)
    var clubId: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 64)
    var permission: ClubPermission = ClubPermission.CLUB_CATALOG_MANAGE
) : Serializable

@Entity
@Table(name = "club_user_permission_overrides")
class ClubUserPermissionOverrideEntity(
    @EmbeddedId
    var id: ClubUserPermissionOverrideId,

    @Column(nullable = false)
    var granted: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
