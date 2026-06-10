package com.club.backend.service

import com.club.backend.domain.entity.ClubEntity
import com.club.backend.domain.entity.SeatEntity
import com.club.backend.domain.enum.SeatType
import com.club.backend.repository.BookingRepository
import com.club.backend.repository.ClubUserBlockRepository
import com.club.backend.repository.SeatRepository
import com.club.backend.repository.projection.BusySeatProjection
import com.club.backend.repository.projection.SeatBookingWindowProjection
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SeatServiceTest {

    private val club = ClubEntity(
        id = 1L,
        name = "Cyber Arena",
        addressFull = "Москва, Тверская, 1",
        addressShort = "Тверская, 1"
    )

    private val seats = listOf(
        SeatEntity(id = 1L, club = club, label = "A1", type = SeatType.REGULAR, sortOrder = 1),
        SeatEntity(id = 2L, club = club, label = "V1", type = SeatType.VIP, sortOrder = 2)
    )

    private val seatRepository: SeatRepository = mock(SeatRepository::class.java)
    private val bookingRepository: BookingRepository = mock(BookingRepository::class.java)
    private val clubAccessService = ClubAccessService(mock(ClubUserBlockRepository::class.java))
    private val service = SeatService(seatRepository, bookingRepository, clubAccessService)

    @Test
    fun `availability marks booked seats as unavailable`() {
        val start = LocalDateTime.of(2026, 5, 20, 18, 0)
        val end = LocalDateTime.of(2026, 5, 20, 20, 0)
        `when`(seatRepository.findAllByClubIdAndIsActiveTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(seats)
        `when`(bookingRepository.findBusySeatIds(1L, start, end)).thenReturn(listOf(BusySeat(2L)))

        val availability = service.getAvailability(userId = null, clubId = 1L, startAt = start, endAt = end)

        assertEquals(2, availability.size)
        assertTrue(availability.first { it.seatId == 1L }.isAvailable)
        assertFalse(availability.first { it.seatId == 2L }.isAvailable)
    }

    @Test
    fun `availability rejects interval with end before start`() {
        val start = LocalDateTime.of(2026, 5, 20, 20, 0)
        val end = LocalDateTime.of(2026, 5, 20, 18, 0)

        assertFailsWith<IllegalArgumentException> {
            service.getAvailability(userId = null, clubId = 1L, startAt = start, endAt = end)
        }
    }

    @Test
    fun `max availability reports current conflict and next booking window`() {
        val start = LocalDateTime.of(2026, 5, 20, 18, 0)
        `when`(seatRepository.findAllByClubIdAndIsActiveTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(seats)
        `when`(bookingRepository.findSeatBookingWindowsAfter(1L, start)).thenReturn(
            listOf(
                BookingWindow(1L, start.minusMinutes(30), start.plusMinutes(30)),
                BookingWindow(2L, start.plusMinutes(90), start.plusMinutes(150))
            )
        )

        val result = service.getMaxAvailability(userId = null, clubId = 1L, startAt = start)

        val busyAtStart = result.first { it.seatId == 1L }
        assertFalse(busyAtStart.isAvailableAtStart)
        assertEquals(0, busyAtStart.maxAvailableMinutes)
        assertNull(busyAtStart.nextBookingStartsAt)

        val availableUntilNextBooking = result.first { it.seatId == 2L }
        assertTrue(availableUntilNextBooking.isAvailableAtStart)
        assertEquals(90, availableUntilNextBooking.maxAvailableMinutes)
        assertEquals(start.plusMinutes(90), availableUntilNextBooking.nextBookingStartsAt)
    }

    private class BusySeat(private val seatId: Long) : BusySeatProjection {
        override fun getSeatId(): Long = seatId
    }

    private class BookingWindow(
        private val seatId: Long,
        private val startAt: LocalDateTime,
        private val endAt: LocalDateTime
    ) : SeatBookingWindowProjection {
        override fun getSeatId(): Long = seatId
        override fun getStartAt(): LocalDateTime = startAt
        override fun getEndAt(): LocalDateTime = endAt
    }
}
