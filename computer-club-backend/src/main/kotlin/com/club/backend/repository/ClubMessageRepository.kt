package com.club.backend.repository

import com.club.backend.domain.entity.ClubMessageEntity
import com.club.backend.domain.entity.ClubMessageType
import com.club.backend.domain.enum.ClubReportStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubMessageRepository : JpaRepository<ClubMessageEntity, Long> {
    fun findAllByClub_IdAndMessageTypeOrderByCreatedAtDesc(
        clubId: Long,
        messageType: ClubMessageType
    ): List<ClubMessageEntity>

    fun findAllByClub_IdAndMessageTypeAndStatusOrderByCreatedAtDesc(
        clubId: Long,
        messageType: ClubMessageType,
        status: ClubReportStatus
    ): List<ClubMessageEntity>

    @Query(
        """
        SELECT m.club.id, COUNT(m)
        FROM ClubMessageEntity m
        WHERE m.messageType = com.club.backend.domain.entity.ClubMessageType.USER_REPORT
        GROUP BY m.club.id
        """
    )
    fun countReportsByClubGrouped(): List<Array<Any>>

    @Query(
        """
        SELECT m
        FROM ClubMessageEntity m
        JOIN FETCH m.club
        WHERE m.messageType = com.club.backend.domain.entity.ClubMessageType.USER_REPORT
          AND m.author.id = :userId
        ORDER BY m.createdAt DESC
        """
    )
    fun findAllReportsByUserIdFetch(@Param("userId") userId: Long): List<ClubMessageEntity>

    @Query(
        """
        SELECT m
        FROM ClubMessageEntity m
        WHERE m.messageType = com.club.backend.domain.entity.ClubMessageType.USER_REPORT
          AND m.club.id = :clubId
          AND m.author.id = :userId
        ORDER BY m.createdAt DESC
        """
    )
    fun findAllReportsByClubIdAndUserIdFetch(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long
    ): List<ClubMessageEntity>
}
