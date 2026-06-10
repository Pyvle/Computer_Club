package com.club.backend.service

import com.club.backend.api.dto.admin.AdminBookingResponse
import com.club.backend.api.dto.admin.AdminPurchaseResponse
import com.club.backend.api.dto.admin.BlockClubRequest
import com.club.backend.api.dto.admin.ClubWarningRequest
import com.club.backend.api.dto.admin.ClubWarningResponse
import com.club.backend.api.dto.admin.GlobalClubBlockResponse
import com.club.backend.api.dto.admin.GlobalClubDetailsResponse
import com.club.backend.api.dto.admin.GlobalClubFloorplanResponse
import com.club.backend.api.dto.admin.GlobalClubPermissionOverrideResponse
import com.club.backend.api.dto.admin.GlobalClubResponse
import com.club.backend.api.dto.admin.GlobalClubStaffDetailsResponse
import com.club.backend.api.dto.admin.GlobalClubStatsResponse
import com.club.backend.domain.entity.ClubMessageEntity
import com.club.backend.domain.entity.ClubMessageType
import com.club.backend.domain.enum.ClubReportStatus
import com.club.backend.domain.enum.FloorplanStatus
import com.club.backend.repository.ClubMessageRepository
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GlobalClubAdminService(
    private val clubRepo: ClubRepository,
    private val clubMessageRepository: ClubMessageRepository,
    private val userRepository: UserRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val clubFloorplanRepository: com.club.backend.repository.ClubFloorplanRepository,
    private val seatAdminService: SeatAdminService,
    private val seatPriceAdminService: SeatPriceAdminService,
    private val clubSeatSpecService: ClubSeatSpecService,
    private val timePackageAdminService: TimePackageAdminService,
    private val clubCatalogAdminService: ClubCatalogAdminService,
    private val clubUserReportService: ClubUserReportService,
    private val clubUserBlockAdminService: ClubUserBlockAdminService,
    private val auditQueryService: AuditQueryService,
    private val clubReportsService: ClubReportsService,
    private val clubStaffPermissionsService: ClubStaffPermissionsService,
    private val objectMapper: ObjectMapper
) {

    fun listAll(): List<GlobalClubResponse> =
        clubRepo.findAllByOrderByIdDesc().let { clubs ->
            val reportCounts = clubMessageRepository.countReportsByClubGrouped().associate { row ->
                (row[0] as Long) to (row[1] as Long).toInt()
            }
            clubs.map { it.toResponse(reportCounts[it.id] ?: 0) }
        }

    @Transactional(readOnly = true)
    fun getDetails(clubId: Long): GlobalClubDetailsResponse {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        val staffRows = clubStaffRepository.findAllByIdClubId(clubId)
        val seats = seatAdminService.list(clubId)
        val seatPrices = seatPriceAdminService.list(clubId)
        val seatSpecs = clubSeatSpecService.getByClub(clubId)
        val timePackages = timePackageAdminService.list(clubId)
        val catalog = clubCatalogAdminService.getClubCatalog(clubId)
        val reports = clubUserReportService.getUserReports(clubId)
        val warnings = getWarnings(clubId)
        val blocks = clubUserBlockAdminService.list(clubId).map {
            GlobalClubBlockResponse(
                userId = it.userId,
                phone = it.phone,
                isBlocked = it.isBlocked,
                reason = it.reason,
                blockedUntil = it.blockedUntil?.toString(),
                blockedByUserId = it.blockedByUserId,
                blockedByPhone = it.blockedByPhone,
                createdAt = it.createdAt.toString(),
                updatedAt = it.updatedAt.toString()
            )
        }
        val bookings = clubReportsService.bookings(clubId, null, null, null)
        val purchases = clubReportsService.purchases(clubId, null, null, null)
        val dashboard = clubReportsService.dashboard(clubId, includeExtendedRevenue = true)
        val audit = auditQueryService.listForClub(clubId, null, null, null, 50)
        val floorplans = clubFloorplanRepository.findAllByClubIdOrderByUpdatedAtDesc(clubId).map { floorplan ->
            GlobalClubFloorplanResponse(
                id = floorplan.id,
                clubId = floorplan.clubId,
                name = floorplan.name,
                status = floorplan.status,
                width = floorplan.width,
                height = floorplan.height,
                gridSize = floorplan.gridSize,
                version = floorplan.version,
                itemCount = floorplan.countItems(),
                data = objectMapper.convertValue(floorplan.data, Any::class.java),
                updatedAt = floorplan.updatedAt.toString()
            )
        }
        val staff = staffRows.map { row ->
            val permissions = clubStaffPermissionsService.get(clubId, row.user.id!!)
            GlobalClubStaffDetailsResponse(
                userId = row.user.id!!,
                phone = row.user.phone,
                role = row.role,
                addedAt = row.createdAt.toString(),
                addedByUserId = row.addedByUser?.id,
                addedByPhone = row.addedByUser?.phone,
                rolePermissions = permissions.rolePermissions.map { it.name },
                overrides = permissions.overrides.map { GlobalClubPermissionOverrideResponse(it.permission.name, it.granted) },
                effectivePermissions = permissions.effectivePermissions.map { it.name }
            )
        }.sortedWith(compareBy<GlobalClubStaffDetailsResponse> { it.role.name != "OWNER" }.thenBy { it.userId })

        return GlobalClubDetailsResponse(
            id = club.id!!,
            name = club.name,
            addressShort = club.addressShort,
            addressFull = club.addressFull,
            locationText = club.locationText,
            description = club.description,
            imageUrl = club.imageUrl,
            isActive = club.isActive,
            isBlocked = club.isBlocked,
            blockReason = club.blockReason,
            latitude = club.latitude,
            longitude = club.longitude,
            createdAt = club.createdAt.toString(),
            updatedAt = club.updatedAt.toString(),
            stats = buildStats(staff, seats, timePackages, floorplans, catalog.products, reports, warnings, blocks, bookings, purchases),
            dashboard = dashboard,
            staff = staff,
            seats = seats,
            seatPrices = seatPrices,
            seatSpecs = seatSpecs,
            timePackages = timePackages,
            floorplans = floorplans,
            catalog = catalog,
            reports = reports,
            warnings = warnings,
            blocks = blocks,
            bookings = bookings,
            purchases = purchases,
            audit = audit
        )
    }

    @Transactional
    fun blockClub(clubId: Long, req: BlockClubRequest) {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        club.isBlocked = true
        club.blockReason = req.reason
    }

    @Transactional
    fun unblockClub(clubId: Long) {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        club.isBlocked = false
        club.blockReason = null
    }

    @Transactional
    fun deleteClub(clubId: Long) {
        if (!clubRepo.existsById(clubId)) throw NoSuchElementException("Club $clubId not found")
        clubRepo.deleteById(clubId)
    }

    @Transactional
    fun addWarning(clubId: Long, adminId: Long, req: ClubWarningRequest): ClubWarningResponse {
        val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
        val admin = userRepository.findById(adminId).orElseThrow { NoSuchElementException("User $adminId not found") }
        val warning = clubMessageRepository.save(
            ClubMessageEntity(
                club = club,
                author = admin,
                messageType = ClubMessageType.PLATFORM_WARNING,
                message = req.message
            )
        )
        return warning.toResponse()
    }

    fun getWarnings(clubId: Long): List<ClubWarningResponse> =
        clubMessageRepository.findAllByClub_IdAndMessageTypeOrderByCreatedAtDesc(clubId, ClubMessageType.PLATFORM_WARNING)
            .map { it.toResponse() }

    private fun buildStats(
        staff: List<GlobalClubStaffDetailsResponse>,
        seats: List<com.club.backend.api.dto.admin.AdminSeatResponse>,
        timePackages: List<com.club.backend.api.dto.admin.AdminTimePackageResponse>,
        floorplans: List<GlobalClubFloorplanResponse>,
        products: List<com.club.backend.api.dto.admin.AdminClubCatalogProductResponse>,
        reports: List<com.club.backend.api.dto.ClubUserReportResponse>,
        warnings: List<ClubWarningResponse>,
        blocks: List<GlobalClubBlockResponse>,
        bookings: List<AdminBookingResponse>,
        purchases: List<AdminPurchaseResponse>
    ): GlobalClubStatsResponse {
        val now = LocalDateTime.now()
        return GlobalClubStatsResponse(
            ownersCount = staff.count { it.role.name == "OWNER" },
            adminsCount = staff.count { it.role.name == "ADMIN" },
            totalSeats = seats.size,
            activeSeats = seats.count { it.isActive },
            regularSeats = seats.count { it.type.name == "REGULAR" },
            vipSeats = seats.count { it.type.name == "VIP" },
            floorplansTotal = floorplans.size,
            publishedFloorplans = floorplans.count { it.status == FloorplanStatus.PUBLISHED },
            draftFloorplans = floorplans.count { it.status == FloorplanStatus.DRAFT },
            archivedFloorplans = floorplans.count { it.status == FloorplanStatus.ARCHIVED },
            linkedCatalogItems = products.count { it.isLinkedToClub },
            availableCatalogItems = products.count { it.clubIsAvailable == true },
            timePackagesTotal = timePackages.size,
            activeTimePackages = timePackages.count { it.isActive },
            activeBlocksCount = blocks.count { it.isBlocked && (it.blockedUntil == null || LocalDateTime.parse(it.blockedUntil).isAfter(now)) },
            warningsCount = warnings.size,
            reportsNewCount = reports.count { it.status == ClubReportStatus.NEW },
            reportsInProgressCount = reports.count { it.status == ClubReportStatus.IN_PROGRESS },
            reportsResolvedCount = reports.count { it.status == ClubReportStatus.RESOLVED },
            bookingsTotal = bookings.size,
            purchasesTotal = purchases.size,
            paidRevenueRub = purchases.filter { it.paymentStatus.name == "PAID" }.sumOf { it.totalAmountRub.toLong() }
        )
    }

    private fun com.club.backend.domain.entity.ClubEntity.toResponse(reportsCount: Int) = GlobalClubResponse(
        id = id!!,
        name = name,
        addressShort = addressShort,
        addressFull = addressFull,
        description = description,
        imageUrl = imageUrl,
        isActive = isActive,
        isBlocked = isBlocked,
        blockReason = blockReason,
        reportsCount = reportsCount,
        createdAt = createdAt.toString()
    )

    private fun ClubMessageEntity.toResponse() = ClubWarningResponse(
        id = id!!,
        message = message,
        createdBy = author!!.id!!,
        createdAt = createdAt
    )

    private fun com.club.backend.domain.entity.ClubFloorplanEntity.countItems(): Int = when {
        data.has("items") && data["items"].isArray -> data["items"].size()
        data.isArray -> data.size()
        else -> 0
    }
}
