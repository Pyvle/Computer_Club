package com.club.backend.repository

import com.club.backend.domain.entity.SeatEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeatRepository : JpaRepository<SeatEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatEntity s WHERE s.id IN :ids ORDER BY s.id")
    fun findAllByIdForUpdate(@Param("ids") ids: List<Long>): List<SeatEntity>
    fun findAllByClubIdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId: Long): List<SeatEntity>

    fun findAllByClubIdOrderBySortOrderAscIdAsc(clubId: Long): List<SeatEntity>

    fun existsByClubIdAndLabel(clubId: Long, label: String): Boolean

    fun existsByIdAndClubId(id: Long, clubId: Long): Boolean

    fun countByClubIdAndIsActiveTrue(clubId: Long): Long
}