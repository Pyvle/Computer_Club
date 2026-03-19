package com.club.backend.repository

import com.club.backend.domain.entity.ClubSeatSpecEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubSeatSpecRepository : JpaRepository<ClubSeatSpecEntity, Long> {
    fun findAllByClubId(clubId: Long): List<ClubSeatSpecEntity>
    fun findByClubIdAndSeatType(clubId: Long, seatType: String): ClubSeatSpecEntity?
}
