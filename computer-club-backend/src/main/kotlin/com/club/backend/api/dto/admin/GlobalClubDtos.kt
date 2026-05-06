package com.club.backend.api.dto.admin

import com.club.backend.api.dto.ClubUserReportResponse
import com.club.backend.domain.entity.ClubRole
import com.club.backend.domain.enum.FloorplanStatus
import java.time.Instant

data class GlobalClubResponse(
    val id: Long,
    val name: String,
    val addressShort: String,
    val addressFull: String,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean,
    val isBlocked: Boolean,
    val blockReason: String?,
    val reportsCount: Int,
    val createdAt: String
)

data class BlockClubRequest(
    val reason: String?
)

data class ClubWarningRequest(
    val message: String
)

data class ClubWarningResponse(
    val id: Long,
    val message: String,
    val createdBy: Long,
    val createdAt: Instant
)

data class GlobalClubStatsResponse(
    val ownersCount: Int,
    val adminsCount: Int,
    val totalSeats: Int,
    val activeSeats: Int,
    val regularSeats: Int,
    val vipSeats: Int,
    val floorplansTotal: Int,
    val publishedFloorplans: Int,
    val draftFloorplans: Int,
    val archivedFloorplans: Int,
    val linkedCatalogItems: Int,
    val availableCatalogItems: Int,
    val timePackagesTotal: Int,
    val activeTimePackages: Int,
    val activeBlocksCount: Int,
    val warningsCount: Int,
    val reportsNewCount: Int,
    val reportsInProgressCount: Int,
    val reportsResolvedCount: Int,
    val bookingsTotal: Int,
    val purchasesTotal: Int,
    val paidRevenueRub: Long
)

data class GlobalClubPermissionOverrideResponse(
    val permission: String,
    val granted: Boolean
)

data class GlobalClubStaffDetailsResponse(
    val userId: Long,
    val phone: String?,
    val role: ClubRole,
    val addedAt: String,
    val addedByUserId: Long?,
    val addedByPhone: String?,
    val rolePermissions: List<String>,
    val overrides: List<GlobalClubPermissionOverrideResponse>,
    val effectivePermissions: List<String>
)

data class GlobalClubBlockResponse(
    val userId: Long,
    val phone: String?,
    val isBlocked: Boolean,
    val reason: String?,
    val blockedUntil: String?,
    val blockedByUserId: Long?,
    val blockedByPhone: String?,
    val createdAt: String,
    val updatedAt: String
)

data class GlobalClubFloorplanResponse(
    val id: Long,
    val clubId: Long,
    val name: String,
    val status: FloorplanStatus,
    val width: Int,
    val height: Int,
    val gridSize: Int,
    val version: Int,
    val itemCount: Int,
    val data: Any?,
    val updatedAt: String
)

data class GlobalClubDetailsResponse(
    val id: Long,
    val name: String,
    val addressShort: String,
    val addressFull: String,
    val locationText: String?,
    val description: String?,
    val imageUrl: String?,
    val isActive: Boolean,
    val isBlocked: Boolean,
    val blockReason: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String,
    val updatedAt: String,
    val stats: GlobalClubStatsResponse,
    val dashboard: ClubDashboardResponse,
    val staff: List<GlobalClubStaffDetailsResponse>,
    val seats: List<AdminSeatResponse>,
    val seatPrices: List<AdminSeatPriceResponse>,
    val seatSpecs: List<SeatSpecResponse>,
    val timePackages: List<AdminTimePackageResponse>,
    val floorplans: List<GlobalClubFloorplanResponse>,
    val catalog: AdminClubCatalogResponse,
    val reports: List<ClubUserReportResponse>,
    val warnings: List<ClubWarningResponse>,
    val blocks: List<GlobalClubBlockResponse>,
    val bookings: List<AdminBookingResponse>,
    val purchases: List<AdminPurchaseResponse>,
    val audit: List<AuditLogResponse>
)
