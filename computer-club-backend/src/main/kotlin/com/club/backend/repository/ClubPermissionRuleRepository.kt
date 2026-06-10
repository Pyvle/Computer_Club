package com.club.backend.repository

import com.club.backend.domain.entity.ClubPermissionRuleEntity
import com.club.backend.domain.entity.ClubPermissionRuleType
import com.club.backend.domain.entity.ClubRole
import com.club.backend.domain.enum.ClubPermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ClubPermissionRuleRepository : JpaRepository<ClubPermissionRuleEntity, Long> {
    fun existsByRuleTypeAndRoleAndPermissionAndGrantedTrue(
        ruleType: ClubPermissionRuleType,
        role: ClubRole,
        permission: ClubPermission
    ): Boolean

    fun findAllByRuleTypeAndRoleAndGrantedTrue(
        ruleType: ClubPermissionRuleType,
        role: ClubRole
    ): List<ClubPermissionRuleEntity>

    @Query(
        """
        select r
        from ClubPermissionRuleEntity r
        where r.ruleType = com.club.backend.domain.entity.ClubPermissionRuleType.USER_OVERRIDE
          and r.club.id = :clubId
          and r.user.id = :userId
        """
    )
    fun findOverridesByClubIdAndUserId(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long
    ): List<ClubPermissionRuleEntity>

    @Query(
        """
        select r
        from ClubPermissionRuleEntity r
        where r.ruleType = com.club.backend.domain.entity.ClubPermissionRuleType.USER_OVERRIDE
          and r.club.id = :clubId
          and r.user.id = :userId
          and r.permission = :permission
        """
    )
    fun findOverride(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long,
        @Param("permission") permission: ClubPermission
    ): Optional<ClubPermissionRuleEntity>
}
