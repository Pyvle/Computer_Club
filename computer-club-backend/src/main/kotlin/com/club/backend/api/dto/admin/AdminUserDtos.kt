package com.club.backend.api.dto.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class AdminUserResponse(
    val id: Long,
    val phone: String?,
    val isActive: Boolean,
    val globalRole: String,
    val hasPassword: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val bookingsCount: Long = 0,
    val purchasesCount: Long = 0,
    val totalSpentRub: Long = 0,
    val visitedClubsCount: Long = 0,
    val lastActivityAt: LocalDateTime? = null,
    val clubRoles: List<UserClubRoleInfo> = emptyList()
)

data class UserPurchasePreview(
    val purchaseId: Long,
    val clubId: Long,
    val clubName: String,
    val totalRub: Int,
    val paymentStatus: String,
    val createdAt: LocalDateTime
)

data class UserBookingPreview(
    val bookingId: Long,
    val clubId: Long,
    val clubName: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: String,
    val totalRub: Int
)

data class UserActiveBlockInfo(
    val clubId: Long,
    val clubName: String,
    val reason: String?,
    val blockedUntil: LocalDateTime?,
    val createdAt: LocalDateTime
)

data class UserClubRoleInfo(
    val clubId: Long,
    val clubName: String,
    val role: String
)

data class AdminUserDetailsResponse(
    val id: Long,
    val phone: String?,
    val isActive: Boolean,
    val globalRole: String,
    val hasPassword: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val bookingsCount: Long,
    val purchasesCount: Long,
    val totalSpentRub: Long,
    val visitedClubsCount: Long,
    val lastActivityAt: LocalDateTime?,
    val recentPurchases: List<UserPurchasePreview>,
    val activeBlocks: List<UserActiveBlockInfo>,
    val recentBookings: List<UserBookingPreview>,
    val clubRoles: List<UserClubRoleInfo>
)

data class GlobalAdminUserBookingItem(
    val bookingId: Long,
    val clubId: Long,
    val clubName: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: String,
    val totalRub: Int,
    val seatLabels: List<String>,
    val purchaseId: Long?
)

data class GlobalAdminUserPurchaseItem(
    val purchaseId: Long,
    val clubId: Long,
    val clubName: String,
    val paymentStatus: String,
    val totalRub: Int,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val createdAt: LocalDateTime
)

data class CreateUserRequest(
    @field:NotBlank val phone: String,
    @field:NotBlank
    @field:Size(min = 6)
    val password: String,
    val globalRole: String = "USER"
)

data class SetActiveRequest(val isActive: Boolean)

data class GlobalAdminUserReportItem(
    val reportId: Long,
    val clubId: Long,
    val clubName: String,
    val message: String,
    val status: String,
    val createdAt: java.time.Instant
)
