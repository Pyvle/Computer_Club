package com.club.backend.repository

import com.club.backend.domain.entity.ClubSeatTypeSettingEntity
import com.club.backend.domain.enum.SeatType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ClubSeatTypeSettingRepository : JpaRepository<ClubSeatTypeSettingEntity, Long> {
    fun findAllByClub_Id(clubId: Long): List<ClubSeatTypeSettingEntity>
    fun findByClub_IdAndSeatType(clubId: Long, seatType: SeatType): Optional<ClubSeatTypeSettingEntity>

    @Query(
        """
        SELECT s.club.id, MIN(s.pricePerHourRub)
        FROM ClubSeatTypeSettingEntity s
        WHERE s.pricePerHourRub IS NOT NULL
        GROUP BY s.club.id
        """
    )
    fun findMinPricePerClub(): List<Array<Any>>
}
