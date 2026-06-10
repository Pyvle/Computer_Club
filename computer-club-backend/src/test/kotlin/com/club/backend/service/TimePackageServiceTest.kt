package com.club.backend.service

import com.club.backend.domain.entity.ClubEntity
import com.club.backend.domain.entity.ClubTimePackageEntity
import com.club.backend.repository.ClubTimePackageRepository
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TimePackageServiceTest {

    @Test
    fun `listActive maps active time packages with total price and time window`() {
        val club = ClubEntity(
            id = 1L,
            name = "Cyber Arena",
            addressFull = "Москва, Тверская, 1",
            addressShort = "Тверская, 1"
        )
        val repo: ClubTimePackageRepository = mock(ClubTimePackageRepository::class.java)
        `when`(repo.findAllByClub_IdAndIsActiveTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(
            listOf(
                ClubTimePackageEntity(
                    id = 10L,
                    club = club,
                    name = "Вечерний пакет",
                    hours = 3,
                    pricePerHourRub = 180,
                    availableFrom = LocalTime.of(18, 0),
                    availableTo = LocalTime.of(23, 30)
                )
            )
        )

        val result = TimePackageService(repo).listActive(1L)

        assertEquals(1, result.size)
        assertEquals(10L, result.single().id)
        assertEquals("Вечерний пакет", result.single().name)
        assertEquals(3, result.single().hours)
        assertEquals(180, result.single().pricePerHourRub)
        assertEquals(540, result.single().totalPriceRub)
        assertEquals("18:00", result.single().availableFrom)
        assertEquals("23:30", result.single().availableTo)
    }
}
