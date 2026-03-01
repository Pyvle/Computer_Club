package com.club.backend.repository

import com.club.backend.domain.entity.ClubRole
import com.club.backend.domain.entity.ClubRolePermissionEntity
import com.club.backend.domain.entity.ClubRolePermissionId
import com.club.backend.domain.enum.ClubPermission
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRolePermissionRepository : JpaRepository<ClubRolePermissionEntity, ClubRolePermissionId> {
    fun existsByIdRoleAndIdPermission(role: ClubRole, permission: ClubPermission): Boolean
    fun findAllByIdRole(role: ClubRole): List<ClubRolePermissionEntity>
}
