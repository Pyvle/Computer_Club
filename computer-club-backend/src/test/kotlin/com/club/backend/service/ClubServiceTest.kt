package com.club.backend.service

import com.club.backend.domain.entity.ClubEntity
import com.club.backend.domain.entity.ClubSeatTypeSettingEntity
import com.club.backend.domain.entity.ClubUserBlockEntity
import com.club.backend.domain.entity.ClubUserBlockId
import com.club.backend.domain.entity.UserEntity
import com.club.backend.domain.enum.SeatType
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubSeatTypeSettingRepository
import com.club.backend.repository.ClubUserBlockRepository
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClubServiceTest {

    private val clubsById = mutableMapOf<Long, ClubEntity>()
    private var activeClubs: List<ClubEntity> = emptyList()
    private var activeBlocks: List<ClubUserBlockEntity> = emptyList()
    private var minPriceRows: List<Array<Any>> = emptyList()
    private var pricesByClubId: Map<Long, List<ClubSeatTypeSettingEntity>> = emptyMap()

    private val clubRepository: ClubRepository = proxyRepo { method, args ->
        when (method) {
            "findById" -> Optional.ofNullable(clubsById[args[0] as Long])
            "findAllByIsActiveTrueAndIsBlockedFalseOrderByIdAsc" -> activeClubs
            else -> unsupported(method)
        }
    }
    private val clubUserBlockRepository: ClubUserBlockRepository = proxyRepo { method, args ->
        when (method) {
            "findActiveBlocksForUser" -> {
                val userId = args[0] as Long
                activeBlocks.filter { it.user.id == userId }
            }
            else -> unsupported(method)
        }
    }
    private val clubSeatTypeSettingRepository: ClubSeatTypeSettingRepository = proxyRepo { method, args ->
        when (method) {
            "findAllByClub_Id" -> pricesByClubId[args[0] as Long].orEmpty()
            "findMinPricePerClub" -> minPriceRows
            else -> unsupported(method)
        }
    }
    private val service = ClubService(clubRepository, clubUserBlockRepository, clubSeatTypeSettingRepository)

    @Test
    fun `getById returns club with minimum seat price`() {
        val club = club(id = 1L, name = "Cyber Arena")
        clubsById[1L] = club
        pricesByClubId = mapOf(
            1L to listOf(
                seatPrice(club, SeatType.VIP, 450),
                seatPrice(club, SeatType.REGULAR, 250),
                seatPrice(club, SeatType.REGULAR, null)
            )
        )

        val result = service.getById(1L)

        assertEquals(1L, result.id)
        assertEquals("Cyber Arena", result.name)
        assertEquals("Тверская, 1", result.address)
        assertEquals(250, result.minPricePerHourRub)
    }

    @Test
    fun `getAvailableForUser marks blocked clubs and attaches minimum prices`() {
        val firstClub = club(id = 1L, name = "Arena One")
        val secondClub = club(id = 2L, name = "Arena Two")
        val blockedUser = UserEntity(id = 77L, phone = "+79990001122")
        val blockedUntil = LocalDateTime.of(2026, 5, 20, 23, 0)

        activeClubs = listOf(firstClub, secondClub)
        minPriceRows = listOf(
            arrayOf(1L, 200),
            arrayOf(2L, 350)
        )
        activeBlocks = listOf(
            ClubUserBlockEntity(
                id = ClubUserBlockId(clubId = 2L, userId = 77L),
                club = secondClub,
                user = blockedUser,
                reason = "Нарушение правил клуба",
                blockedUntil = blockedUntil
            )
        )

        val result = service.getAvailableForUser(77L)

        assertEquals(2, result.size)
        val availableClub = result.first { it.id == 1L }
        assertFalse(availableClub.isBlocked)
        assertEquals(200, availableClub.minPricePerHourRub)

        val blockedClub = result.first { it.id == 2L }
        assertTrue(blockedClub.isBlocked)
        assertEquals("Нарушение правил клуба", blockedClub.blockReason)
        assertEquals(blockedUntil, blockedClub.blockedUntil)
        assertEquals(350, blockedClub.minPricePerHourRub)
    }

    private fun club(id: Long, name: String) = ClubEntity(
        id = id,
        name = name,
        addressFull = "Москва, Тверская, 1",
        addressShort = "Тверская, 1",
        locationText = "Центр",
        description = "Клуб для тестов",
        latitude = 55.7558,
        longitude = 37.6176
    )

    private fun seatPrice(club: ClubEntity, seatType: SeatType, price: Int?) = ClubSeatTypeSettingEntity(
        id = null,
        club = club,
        seatType = seatType,
        pricePerHourRub = price,
        title = seatType.name,
        specsJson = "[]"
    )
}
