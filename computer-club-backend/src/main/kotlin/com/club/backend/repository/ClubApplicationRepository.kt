package com.club.backend.repository

import com.club.backend.domain.entity.ClubApplicationEntity
import com.club.backend.domain.entity.ClubApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubApplicationRepository : JpaRepository<ClubApplicationEntity, Long> {

    fun findAllByApplicant_IdOrderByCreatedAtDesc(applicantUserId: Long): List<ClubApplicationEntity>

    fun existsByApplicant_IdAndStatus(applicantUserId: Long, status: ClubApplicationStatus): Boolean

    fun findAllByStatusOrderByCreatedAtDesc(status: ClubApplicationStatus): List<ClubApplicationEntity>

    @Query(
        """
        select a from ClubApplicationEntity a
        where (:status is null or a.status = :status)
        order by a.createdAt desc
        """
    )
    fun findAllFiltered(@Param("status") status: ClubApplicationStatus?): List<ClubApplicationEntity>
}
