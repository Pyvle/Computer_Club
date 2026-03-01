package com.club.backend.repository

import com.club.backend.domain.entity.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long> {

    @Query(
        """
        select a from AuditLogEntity a
        where a.club.id = :clubId
        order by a.createdAt desc
        """
    )
    fun findLatestByClubId(@Param("clubId") clubId: Long): List<AuditLogEntity>
}
