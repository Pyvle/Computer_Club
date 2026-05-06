package com.club.backend.repository

import com.club.backend.domain.entity.ClubUserReportEntity
import com.club.backend.domain.enum.ClubReportStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubUserReportRepository : JpaRepository<ClubUserReportEntity, Long> {
    fun findAllByClubIdOrderByCreatedAtDesc(clubId: Long): List<ClubUserReportEntity>
    fun findAllByClubIdAndStatusOrderByCreatedAtDesc(clubId: Long, status: ClubReportStatus): List<ClubUserReportEntity>

    @Query("SELECT r.club.id, COUNT(r) FROM ClubUserReportEntity r GROUP BY r.club.id")
    fun countByClubGrouped(): List<Array<Any>>

    @Query("SELECT r FROM ClubUserReportEntity r JOIN FETCH r.club WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    fun findAllByUserIdFetch(@Param("userId") userId: Long): List<ClubUserReportEntity>

    @Query("SELECT r FROM ClubUserReportEntity r WHERE r.club.id = :clubId AND r.user.id = :userId ORDER BY r.createdAt DESC")
    fun findAllByClubIdAndUserIdFetch(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long
    ): List<ClubUserReportEntity>
}
