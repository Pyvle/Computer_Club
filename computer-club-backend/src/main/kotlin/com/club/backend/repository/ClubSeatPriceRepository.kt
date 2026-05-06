package com.club.backend.repository

import com.club.backend.domain.entity.ClubSeatPriceEntity
import com.club.backend.domain.enum.SeatType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ClubSeatPriceRepository : JpaRepository<ClubSeatPriceEntity, Long> {
    fun findAllByClub_Id(clubId: Long): List<ClubSeatPriceEntity>
    fun findByClub_IdAndSeatType(clubId: Long, seatType: SeatType): Optional<ClubSeatPriceEntity>

    /** Возвращает Map<clubId, minPrice> для всех клубов у которых есть хотя бы одна цена. */
    @Query("SELECT p.club.id, MIN(p.pricePerHourRub) FROM ClubSeatPriceEntity p GROUP BY p.club.id")
    fun findMinPricePerClub(): List<Array<Any>>
}
