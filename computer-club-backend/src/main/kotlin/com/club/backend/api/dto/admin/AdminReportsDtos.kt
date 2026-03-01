package com.club.backend.api.dto.admin

import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus

data class AdminBookingResponse(
    val id: Long,
    val userId: Long,
    val clubId: Long,
    val status: BookingStatus,
    val startAt: String,
    val endAt: String,
    val seatIds: List<Long>
)

data class AdminPurchaseResponse(
    val id: Long,
    val userId: Long,
    val clubId: Long,
    val paymentStatus: PaymentStatus,
    val totalAmountRub: Int,
    val createdAt: String
)
