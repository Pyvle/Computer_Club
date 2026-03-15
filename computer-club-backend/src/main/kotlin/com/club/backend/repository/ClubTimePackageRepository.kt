package com.club.backend.repository

import com.club.backend.domain.entity.ClubTimePackageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubTimePackageRepository : JpaRepository<ClubTimePackageEntity, Long> {
    fun findAllByClub_IdOrderBySortOrderAscIdAsc(clubId: Long): List<ClubTimePackageEntity>
    fun findAllByClub_IdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId: Long): List<ClubTimePackageEntity>
}
