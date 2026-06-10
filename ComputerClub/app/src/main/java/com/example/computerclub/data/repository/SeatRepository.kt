package com.example.computerclub.data.repository

import com.example.computerclub.data.network.SeatApi
import com.example.computerclub.data.network.dto.SeatAvailabilityRequestDto
import com.example.computerclub.data.network.dto.SeatAvailabilityResponseDto
import com.example.computerclub.data.network.dto.SeatMaxAvailabilityRequestDto
import com.example.computerclub.data.network.dto.SeatMaxAvailabilityResponseDto
import com.example.computerclub.data.network.dto.SeatResponseDto
import com.example.computerclub.data.network.dto.SeatSpecResponseDto

class SeatRepository(private val api: SeatApi) {
    suspend fun seats(clubId: Long): List<SeatResponseDto> = api.getSeats(clubId)
    suspend fun seatSpecs(clubId: Long): List<SeatSpecResponseDto> = api.getSeatSpecs(clubId)

    suspend fun availability(clubId: Long, startAtIso: String, endAtIso: String): List<SeatAvailabilityResponseDto> =
        api.getAvailability(clubId, SeatAvailabilityRequestDto(startAtIso, endAtIso))

    suspend fun maxAvailability(clubId: Long, startAtIso: String): List<SeatMaxAvailabilityResponseDto> =
        api.getMaxAvailability(clubId, SeatMaxAvailabilityRequestDto(startAtIso))
}
