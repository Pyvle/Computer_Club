package com.club.backend.repository

import com.club.backend.domain.entity.ClubFloorplanEntity
import com.club.backend.domain.enum.FloorplanStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClubFloorplanRepository : JpaRepository<ClubFloorplanEntity, Long> {

    fun findAllByClubIdOrderByUpdatedAtDesc(clubId: Long): List<ClubFloorplanEntity>

    fun findByClubIdAndStatus(clubId: Long, status: FloorplanStatus): ClubFloorplanEntity?

    fun existsByClubIdAndStatus(clubId: Long, status: FloorplanStatus): Boolean
}
