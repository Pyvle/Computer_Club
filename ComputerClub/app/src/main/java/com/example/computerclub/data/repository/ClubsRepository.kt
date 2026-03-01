package com.example.computerclub.data.repository

import com.example.computerclub.data.network.ClubsApi
import com.example.computerclub.data.network.dto.AvailableClubResponseDto
import com.example.computerclub.data.network.dto.ClubResponseDto

class ClubsRepository(private val api: ClubsApi) {
    suspend fun getClubs(): List<ClubResponseDto> = api.getClubs()
    suspend fun getAvailable(): List<AvailableClubResponseDto> = api.getAvailableClubs()
}