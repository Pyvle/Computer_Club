package com.club.backend.repository

import com.club.backend.domain.entity.ClubSeatPriceEntity
import com.club.backend.domain.enum.SeatType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ClubSeatPriceRepository : JpaRepository<ClubSeatPriceEntity, Long> {
    fun findAllByClub_Id(clubId: Long): List<ClubSeatPriceEntity>
    fun findByClub_IdAndSeatType(clubId: Long, seatType: SeatType): Optional<ClubSeatPriceEntity>
}
