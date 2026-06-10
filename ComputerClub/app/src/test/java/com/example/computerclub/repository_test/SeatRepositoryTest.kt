package com.example.computerclub.repository_test

import com.example.computerclub.data.network.SeatApi
import com.example.computerclub.data.network.dto.SeatAvailabilityRequestDto
import com.example.computerclub.data.network.dto.SeatAvailabilityResponseDto
import com.example.computerclub.data.network.dto.SeatMaxAvailabilityRequestDto
import com.example.computerclub.data.network.dto.SeatMaxAvailabilityResponseDto
import com.example.computerclub.data.network.dto.SeatResponseDto
import com.example.computerclub.data.network.dto.SeatSpecResponseDto
import com.example.computerclub.data.repository.SeatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SeatRepositoryTest {

    @Test
    fun availabilitySendsSelectedTimeRangeToApi() = runBlocking {
        val api = FakeSeatApi()

        SeatRepository(api).availability(
            clubId = 7,
            startAtIso = "2026-05-15T18:00:00",
            endAtIso = "2026-05-15T20:00:00"
        )

        assertEquals(7L, api.lastAvailabilityClubId)
        assertEquals(
            SeatAvailabilityRequestDto(
                startAt = "2026-05-15T18:00:00",
                endAt = "2026-05-15T20:00:00"
            ),
            api.lastAvailabilityRequest
        )
    }

    @Test
    fun maxAvailabilitySendsStartTimeToApi() = runBlocking {
        val api = FakeSeatApi()

        SeatRepository(api).maxAvailability(clubId = 7, startAtIso = "2026-05-15T18:00:00")

        assertEquals(7L, api.lastMaxAvailabilityClubId)
        assertEquals(SeatMaxAvailabilityRequestDto(startAt = "2026-05-15T18:00:00"), api.lastMaxAvailabilityRequest)
    }

    private class FakeSeatApi : SeatApi {
        var lastAvailabilityClubId: Long? = null
        var lastAvailabilityRequest: SeatAvailabilityRequestDto? = null
        var lastMaxAvailabilityClubId: Long? = null
        var lastMaxAvailabilityRequest: SeatMaxAvailabilityRequestDto? = null

        override suspend fun getSeats(clubId: Long): List<SeatResponseDto> =
            listOf(SeatResponseDto(id = 1, label = "A1", type = "REGULAR"))

        override suspend fun getSeatSpecs(clubId: Long): List<SeatSpecResponseDto> = emptyList()

        override suspend fun getAvailability(
            clubId: Long,
            dto: SeatAvailabilityRequestDto
        ): List<SeatAvailabilityResponseDto> {
            lastAvailabilityClubId = clubId
            lastAvailabilityRequest = dto
            return listOf(SeatAvailabilityResponseDto(seatId = 1, label = "A1", type = "REGULAR", isAvailable = true))
        }

        override suspend fun getMaxAvailability(
            clubId: Long,
            dto: SeatMaxAvailabilityRequestDto
        ): List<SeatMaxAvailabilityResponseDto> {
            lastMaxAvailabilityClubId = clubId
            lastMaxAvailabilityRequest = dto
            return listOf(
                SeatMaxAvailabilityResponseDto(
                    seatId = 1,
                    label = "A1",
                    type = "REGULAR",
                    isAvailableAtStart = true,
                    maxAvailableMinutes = 120
                )
            )
        }
    }
}
