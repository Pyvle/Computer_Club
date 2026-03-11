package com.club.backend.repository

import com.club.backend.domain.entity.BookingSeatEntity
import com.club.backend.domain.entity.BookingSeatId
import com.club.backend.domain.enum.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface BookingSeatRepository : JpaRepository<BookingSeatEntity, BookingSeatId> {

    fun existsBySeat_IdAndBooking_StatusIn(seatId: Long, statuses: Collection<BookingStatus>): Boolean

    @Modifying
    @Query("DELETE FROM BookingSeatEntity bs WHERE bs.seat.id = :seatId")
    fun deleteAllBySeatId(seatId: Long)
}