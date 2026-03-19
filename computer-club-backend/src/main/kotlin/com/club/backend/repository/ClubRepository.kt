package com.club.backend.repository

import com.club.backend.domain.entity.ClubEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRepository : JpaRepository<ClubEntity, Long> {
    fun findAllByIsActiveTrueAndIsBlockedFalseOrderByIdAsc(): List<ClubEntity>
    fun findAllByOrderByIdDesc(): List<ClubEntity>
}