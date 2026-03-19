package com.club.backend.repository

import com.club.backend.domain.entity.ClubUserReportEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubUserReportRepository : JpaRepository<ClubUserReportEntity, Long> {
    fun findAllByClubIdOrderByCreatedAtDesc(clubId: Long): List<ClubUserReportEntity>
}
