package com.club.backend.repository

import com.club.backend.domain.entity.ClubWarningEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubWarningRepository : JpaRepository<ClubWarningEntity, Long> {
    fun findAllByClubIdOrderByCreatedAtDesc(clubId: Long): List<ClubWarningEntity>
}
