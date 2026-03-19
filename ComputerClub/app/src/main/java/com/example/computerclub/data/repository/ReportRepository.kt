package com.example.computerclub.data.repository

import com.example.computerclub.data.network.ReportApi
import com.example.computerclub.data.network.dto.CreateReportRequestDto

class ReportRepository(private val api: ReportApi) {
    suspend fun submitReport(clubId: Long, message: String) =
        api.submitReport(clubId, CreateReportRequestDto(message))
}
