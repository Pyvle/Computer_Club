package com.club.backend.api.dto.admin

import java.time.Instant
import java.time.LocalDateTime

data class ClubUserListItem(
    val userId: Long,
    val phone: String?,
    val isActive: Boolean,
    val firstVisitAt: LocalDateTime?,
    val lastVisitAt: LocalDateTime?,
    val bookingsCount: Long,
    val purchasesCount: Long,
    val totalSpentRub: Long,
    val cancelledBookingsCount: Long,
    val isBlocked: Boolean,
    val blockedUntil: LocalDateTime?,
    val blockReason: String?
)

data class ClubUserDetailResponse(
    val userId: Long,
    val phone: String?,
    val isActive: Boolean,
    val isBlocked: Boolean,
    val blockReason: String?,
    val blockedUntil: LocalDateTime?,
    val blockedAt: LocalDateTime?,
    val blockedByPhone: String?,
    val firstVisitAt: LocalDateTime?,
    val lastVisitAt: LocalDateTime?,
    val bookingsCount: Long,
    val purchasesCount: Long,
    val totalSpentRub: Long,
    val avgSpentRub: Long,
    val cancelledBookingsCount: Long,
    val totalHoursBooked: Double,
    val favoriteSeatType: String?,
    val recentBookings: List<ClubUserBookingItem>,
    val recentPurchases: List<ClubUserPurchaseItem>,
    val reports: List<ClubUserReportForDetail>
)

data class ClubUserBookingItem(
    val bookingId: Long,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val seatLabels: List<String>,
    val durationHours: Double,
    val totalRub: Int,
    val status: String
)

data class ClubUserPurchaseItem(
    val purchaseId: Long,
    val createdAt: LocalDateTime,
    val totalRub: Int,
    val paymentStatus: String
)

data class ClubUserReportForDetail(
    val reportId: Long,
    val message: String,
    val status: String,
    val createdAt: Instant
)
