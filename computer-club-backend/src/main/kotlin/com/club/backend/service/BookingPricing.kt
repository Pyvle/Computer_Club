package com.club.backend.service

import com.club.backend.domain.enum.SeatType
import kotlin.math.ceil

internal const val FALLBACK_BOOKING_RATE = 200

internal data class BookingPricing(
    private val seatPriceByType: Map<SeatType, Int>,
    private val packageRateByHours: Map<Int, Int>
) {
    private val standardRate: Int =
        if (seatPriceByType.isNotEmpty()) seatPriceByType.values.min() else FALLBACK_BOOKING_RATE

    fun baseRate(packageHours: Int?): Int =
        if (packageHours != null) packageRateByHours[packageHours] ?: standardRate else standardRate

    fun effectiveRate(seatType: SeatType, packageHours: Int?): Int {
        val seatTypeRate = seatPriceByType[seatType] ?: standardRate
        val surcharge = maxOf(0, seatTypeRate - standardRate)
        return baseRate(packageHours) + surcharge
    }

    fun seatTotalRub(hours: Double, seatType: SeatType, packageHours: Int?): Int =
        ceil(hours * effectiveRate(seatType, packageHours)).toInt()
}
