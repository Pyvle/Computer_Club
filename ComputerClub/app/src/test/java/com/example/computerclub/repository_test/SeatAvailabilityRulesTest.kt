package com.example.computerclub.repository_test

import com.example.computerclub.model.Seat
import com.example.computerclub.model.SeatAvailability
import com.example.computerclub.model.SeatType
import com.example.computerclub.model.TimeRange
import com.example.computerclub.model.seatAvailabilityForSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class SeatAvailabilityRulesTest {

    @Test
    fun seatSelectionDetectsFreeBookedPartialAndCartConflictStates() {
        val seat = regularSeat(booked = listOf(TimeRange(18 * 60, 20 * 60)))

        assertEquals(SeatAvailability.FREE, seatAvailabilityForSelection(seat, TimeRange(15 * 60, 16 * 60)))
        assertEquals(SeatAvailability.BOOKED, seatAvailabilityForSelection(seat, TimeRange(18 * 60 + 30, 19 * 60)))
        assertEquals(SeatAvailability.PARTIAL, seatAvailabilityForSelection(seat, TimeRange(19 * 60, 21 * 60)))
        assertEquals(SeatAvailability.BOOKED, seatAvailabilityForSelection(seat, TimeRange(16 * 60, 17 * 60), hasCartConflict = true))
    }

    @Test
    fun seatSelectionBlocksRiskWindowAfterExistingBooking() {
        val seat = regularSeat(booked = listOf(TimeRange(18 * 60, 20 * 60)))

        assertEquals(SeatAvailability.BOOKED, seatAvailabilityForSelection(seat, TimeRange(20 * 60 + 30, 21 * 60)))
    }

    @Test
    fun seatSelectionSupportsBookingRangesAcrossMidnight() {
        val seat = regularSeat(booked = listOf(TimeRange(23 * 60, 60)))

        assertEquals(SeatAvailability.BOOKED, seatAvailabilityForSelection(seat, TimeRange(23 * 60 + 30, 30)))
        assertEquals(SeatAvailability.FREE, seatAvailabilityForSelection(seat, TimeRange(12 * 60, 13 * 60)))
    }

    private fun regularSeat(booked: List<TimeRange>): Seat =
        Seat(
            id = "1",
            label = "A1",
            type = SeatType.REGULAR,
            hasPc = true,
            equipment = "RTX 4060",
            booked = booked
        )
}
