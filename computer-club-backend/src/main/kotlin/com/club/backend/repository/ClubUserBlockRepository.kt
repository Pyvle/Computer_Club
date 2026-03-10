package com.club.backend.repository

import com.club.backend.domain.entity.ClubUserBlockEntity
import com.club.backend.domain.entity.ClubUserBlockId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ClubUserBlockRepository : JpaRepository<ClubUserBlockEntity, ClubUserBlockId> {

    @Query("""
        select count(b) > 0 from ClubUserBlockEntity b
        where b.club.id = :clubId
          and b.user.id = :userId
          and b.isBlocked = true
          and (b.blockedUntil is null or b.blockedUntil > :now)
    """)
    fun isBlockedNow(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime
    ): Boolean

    @Query("""
        select b from ClubUserBlockEntity b
        left join fetch b.user
        left join fetch b.blockedBy
        where b.id.clubId = :clubId
        order by b.createdAt desc
    """)
    fun findAllByClubIdFetched(@Param("clubId") clubId: Long): List<ClubUserBlockEntity>

    @Query("""
        select b from ClubUserBlockEntity b
        where b.user.id = :userId
          and b.isBlocked = true
          and (b.blockedUntil is null or b.blockedUntil > :now)
    """)
    fun findActiveBlocksForUser(
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime
    ): List<ClubUserBlockEntity>
}
