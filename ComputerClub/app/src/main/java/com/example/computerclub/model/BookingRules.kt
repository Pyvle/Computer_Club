package com.example.computerclub.model

private const val DAY_MIN = 24 * 60
private const val MAX_EXTENSION_MIN = 120

fun bookingMinutes(startDayOffset: Int, startMin: Int, endDayOffset: Int, endMin: Int): Int {
    val startAbs = startDayOffset * DAY_MIN + startMin
    val endAbs = endDayOffset * DAY_MIN + endMin
    return (endAbs - startAbs).coerceAtLeast(0)
}

fun bookingLineCost(line: CartBookingLine, rateRubPerHour: Int): Int {
    val minutes = bookingMinutes(line.startDayOffset, line.startMin, line.endDayOffset, line.endMin)
    if (minutes <= 0 || line.seatIds.isEmpty()) return 0
    val hours = (minutes + 59) / 60
    return rateRubPerHour * hours * line.seatIds.size
}

fun productLinesTotal(lines: List<CartProductLine>): Int =
    lines.sumOf { it.price * it.qty }

fun selectedTimeRangeForSeats(startDayOffset: Int, startMin: Int, endDayOffset: Int, endMin: Int): TimeRange? {
    val minutes = bookingMinutes(startDayOffset, startMin, endDayOffset, endMin)
    if (minutes <= 0) return null
    return if (minutes >= DAY_MIN) TimeRange(0, DAY_MIN) else TimeRange(startMin, endMin)
}

fun seatAvailabilityForSelection(
    seat: Seat,
    selected: TimeRange?,
    hasCartConflict: Boolean = false
): SeatAvailability {
    if (hasCartConflict) return SeatAvailability.BOOKED
    if (selected == null) return SeatAvailability.FREE

    val bookedSegments = seat.booked.flatMap(::segments)
    val selectedSegments = segments(selected)
    if (overlaps(selectedSegments, bookedSegments)) {
        val fullyCovered = seat.booked.any { it.startMin <= selected.startMin && selected.endMin <= it.endMin }
        return if (fullyCovered) SeatAvailability.BOOKED else SeatAvailability.PARTIAL
    }

    val extendedBookedSegments = seat.booked.flatMap { range ->
        segments(range).flatMap { (start, end) ->
            val extendedEnd = end + MAX_EXTENSION_MIN
            if (extendedEnd <= DAY_MIN) listOf(start to extendedEnd)
            else listOf(start to DAY_MIN, 0 to (extendedEnd - DAY_MIN))
        }
    }

    return if (overlaps(selectedSegments, extendedBookedSegments)) SeatAvailability.BOOKED else SeatAvailability.FREE
}

private fun segments(range: TimeRange): List<Pair<Int, Int>> =
    if (range.endMin >= range.startMin) listOf(range.startMin to range.endMin)
    else listOf(range.startMin to DAY_MIN, 0 to range.endMin)

private fun overlaps(a: List<Pair<Int, Int>>, b: List<Pair<Int, Int>>): Boolean =
    a.any { (aStart, aEnd) -> b.any { (bStart, bEnd) -> aStart < bEnd && bStart < aEnd } }
