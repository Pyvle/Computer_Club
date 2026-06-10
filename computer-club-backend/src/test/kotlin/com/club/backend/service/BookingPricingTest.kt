package com.club.backend.service

import com.club.backend.domain.enum.SeatType
import kotlin.test.Test
import kotlin.test.assertEquals

class BookingPricingTest {

    @Test
    fun `package rate replaces standard rate and keeps vip surcharge`() {
        val pricing = BookingPricing(
            seatPriceByType = mapOf(
                SeatType.REGULAR to 100,
                SeatType.VIP to 150
            ),
            packageRateByHours = mapOf(3 to 90)
        )

        assertEquals(90, pricing.effectiveRate(SeatType.REGULAR, packageHours = 3))
        assertEquals(140, pricing.effectiveRate(SeatType.VIP, packageHours = 3))
        assertEquals(270, pricing.seatTotalRub(hours = 3.0, seatType = SeatType.REGULAR, packageHours = 3))
        assertEquals(420, pricing.seatTotalRub(hours = 3.0, seatType = SeatType.VIP, packageHours = 3))
    }

    @Test
    fun `standard rate is used without package`() {
        val pricing = BookingPricing(
            seatPriceByType = mapOf(
                SeatType.REGULAR to 100,
                SeatType.VIP to 150
            ),
            packageRateByHours = emptyMap()
        )

        assertEquals(100, pricing.effectiveRate(SeatType.REGULAR, packageHours = null))
        assertEquals(150, pricing.effectiveRate(SeatType.VIP, packageHours = null))
    }
}
