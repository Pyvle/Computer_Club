package com.club.backend.repository

import com.club.backend.domain.entity.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long> {

    /**
     * Возвращает все записи аудита клуба с предзагрузкой актора.
     * Фильтрация по action/from/to выполняется в сервисе — PostgreSQL не может
     * определить тип nullable-параметра в конструкции `:param is null`.
     */
    @Query(
        """
        select a from AuditLogEntity a
        join fetch a.actor
        where a.club.id = :clubId
        order by a.createdAt desc
        """
    )
    fun findAllByClubId(@Param("clubId") clubId: Long): List<AuditLogEntity>
}
