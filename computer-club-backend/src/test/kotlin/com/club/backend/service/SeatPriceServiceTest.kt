package com.club.backend.service

import com.club.backend.domain.entity.ClubEntity
import com.club.backend.domain.entity.ClubSeatTypeSettingEntity
import com.club.backend.domain.enum.SeatType
import com.club.backend.repository.ClubSeatTypeSettingRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SeatPriceServiceTest {

    private var prices: List<ClubSeatTypeSettingEntity> = emptyList()
    private val repo: ClubSeatTypeSettingRepository = proxyRepo { method, _ ->
        when (method) {
            "findAllByClub_Id" -> prices
            else -> unsupported(method)
        }
    }
    private val service = SeatPriceService(repo)

    @Test
    fun `list filters null prices and sorts by seat type`() {
        val club = ClubEntity(
            id = 1L,
            name = "Arena",
            addressFull = "Москва, Ленина, 10",
            addressShort = "Ленина, 10"
        )
        prices = listOf(
            listOf(
                ClubSeatTypeSettingEntity(club = club, seatType = SeatType.VIP, pricePerHourRub = 450, title = "VIP", specsJson = "[]"),
                ClubSeatTypeSettingEntity(club = club, seatType = SeatType.REGULAR, pricePerHourRub = null, title = "REGULAR", specsJson = "[]"),
                ClubSeatTypeSettingEntity(club = club, seatType = SeatType.REGULAR, pricePerHourRub = 250, title = "REGULAR", specsJson = "[]")
            )
        ).flatten()

        val result = service.list(1L)

        assertEquals(2, result.size)
        assertEquals("REGULAR", result[0].seatType)
        assertEquals(250, result[0].pricePerHourRub)
        assertEquals("VIP", result[1].seatType)
        assertEquals(450, result[1].pricePerHourRub)
    }
}
