package com.example.computerclub.repository_test

import com.example.computerclub.model.CartBookingLine
import com.example.computerclub.model.CartProductLine
import com.example.computerclub.model.TimeRange
import com.example.computerclub.model.bookingLineCost
import com.example.computerclub.model.bookingMinutes
import com.example.computerclub.model.productLinesTotal
import com.example.computerclub.model.selectedTimeRangeForSeats
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookingRulesTest {

    @Test
    fun bookingDurationSupportsSameDayAndNextDayBookings() {
        assertEquals(180, bookingMinutes(0, 18 * 60, 0, 21 * 60))
        assertEquals(120, bookingMinutes(0, 23 * 60, 1, 60))
        assertEquals(0, bookingMinutes(0, 20 * 60, 0, 18 * 60))
    }

    @Test
    fun selectedSeatRangeHandlesInvalidFullDayAndMidnightCases() {
        assertNull(selectedTimeRangeForSeats(0, 20 * 60, 0, 18 * 60))
        assertEquals(TimeRange(0, 24 * 60), selectedTimeRangeForSeats(0, 10 * 60, 1, 10 * 60))
        assertEquals(TimeRange(23 * 60, 60), selectedTimeRangeForSeats(0, 23 * 60, 1, 60))
    }

    @Test
    fun bookingLineCostRoundsDurationUpAndMultipliesBySelectedSeats() {
        val line = CartBookingLine(
            id = "line-1",
            clubId = "1",
            date = LocalDate.of(2026, 5, 14),
            startDayOffset = 0,
            startMin = 18 * 60,
            endDayOffset = 0,
            endMin = 19 * 60 + 15,
            packageHours = null,
            seatIds = listOf("1", "2")
        )

        assertEquals(800, bookingLineCost(line, rateRubPerHour = 200))
    }

    @Test
    fun bookingLineCostReturnsZeroForInvalidDurationOrEmptySeats() {
        val line = CartBookingLine(
            id = "line-1",
            clubId = "1",
            date = LocalDate.of(2026, 5, 14),
            startDayOffset = 0,
            startMin = 20 * 60,
            endDayOffset = 0,
            endMin = 18 * 60,
            packageHours = null,
            seatIds = listOf("1")
        )
        val emptySeatsLine = line.copy(startMin = 18 * 60, endMin = 20 * 60, seatIds = emptyList())

        assertEquals(0, bookingLineCost(line, rateRubPerHour = 200))
        assertEquals(0, bookingLineCost(emptySeatsLine, rateRubPerHour = 200))
    }

    @Test
    fun productLinesTotalUsesActualQuantityAndPrice() {
        val lines = listOf(
            CartProductLine(productId = "1", title = "Water", price = 90, variant = null, qty = 2),
            CartProductLine(productId = "2", title = "Sandwich", price = 250, variant = null, qty = 1)
        )

        assertEquals(430, productLinesTotal(lines))
    }
}
