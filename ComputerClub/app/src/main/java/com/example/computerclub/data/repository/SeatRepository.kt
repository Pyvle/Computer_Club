package com.example.computerclub.data.repository

import com.example.computerclub.data.network.SeatApi
import com.example.computerclub.data.network.dto.SeatAvailabilityRequestDto
import com.example.computerclub.data.network.dto.SeatAvailabilityResponseDto
import com.example.computerclub.data.network.dto.SeatResponseDto

class SeatRepository(private val api: SeatApi) {
    suspend fun seats(clubId: Long): List<SeatResponseDto> = api.getSeats(clubId)

    suspend fun availability(clubId: Long, startAtIso: String, endAtIso: String): List<SeatAvailabilityResponseDto> =
        api.getAvailability(clubId, SeatAvailabilityRequestDto(startAtIso, endAtIso))
}
