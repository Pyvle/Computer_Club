package com.club.backend.repository

import com.club.backend.domain.entity.ClubUserPermissionOverrideEntity
import com.club.backend.domain.entity.ClubUserPermissionOverrideId
import com.club.backend.domain.enum.ClubPermission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ClubUserPermissionOverrideRepository : JpaRepository<ClubUserPermissionOverrideEntity, ClubUserPermissionOverrideId> {
    fun findByIdClubIdAndIdUserId(clubId: Long, userId: Long): List<ClubUserPermissionOverrideEntity>

    fun findByIdClubIdAndIdUserIdAndIdPermission(
        clubId: Long,
        userId: Long,
        permission: ClubPermission
    ): Optional<ClubUserPermissionOverrideEntity>
}
