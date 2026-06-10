package com.club.backend.api.dto

data class UserBookingHistoryItemResponse(
    val bookingId: Long,
    val purchaseId: Long?,
    val clubId: Long,
    val clubName: String,
    val createdAt: String,
    val startAt: String,
    val endAt: String,
    val status: String,
    val totalRub: Int,
    val rateRubPerHourSnapshot: Int,
    val packageHours: Int?,
    val seatIds: List<Long>,
    val seatLabels: List<String>,
    val paymentStatus: String?
)
