package com.club.backend.repository

import com.club.backend.domain.entity.OtpChallengeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface OtpChallengeRepository : JpaRepository<OtpChallengeEntity, Long> {

    @Query("""
        select o from OtpChallengeEntity o
        where o.phone = :phone and o.status = 'PENDING'
        order by o.createdAt desc
    """)
    fun findLatestPendingByPhone(@Param("phone") phone: String): List<OtpChallengeEntity>

    @Query("""
        select count(o) from OtpChallengeEntity o
        where o.phone = :phone and o.createdAt >= :fromTime
    """)
    fun countRequestsSince(@Param("phone") phone: String, @Param("fromTime") fromTime: LocalDateTime): Long

    fun findByIdAndStatus(id: Long, status: String): Optional<OtpChallengeEntity>
}
