package com.club.backend.repository

import com.club.backend.domain.entity.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long> {

    @Query(
        """
        select a from AuditLogEntity a
        join fetch a.actor
        where a.club.id = :clubId
          and (:action is null or a.action = :action)
          and (:from is null or a.createdAt >= :from)
          and (:to is null or a.createdAt <= :to)
        order by a.createdAt desc
        """
    )
    fun findFiltered(
        @Param("clubId") clubId: Long,
        @Param("action") action: String?,
        @Param("from") from: OffsetDateTime?,
        @Param("to") to: OffsetDateTime?
    ): List<AuditLogEntity>
}
