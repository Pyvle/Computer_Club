package com.club.backend.service

import com.club.backend.api.dto.admin.*
import com.club.backend.domain.entity.*
import com.club.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class SetGlobalRoleRequest(val role: String)

@Service
class GlobalAdminService(
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val bookingRepository: BookingRepository,
    private val purchaseRepository: PurchaseRepository,
    private val clubUserBlockRepository: ClubUserBlockRepository,
    private val productOrderItemRepository: ProductOrderItemRepository,
    private val clubMessageRepository: ClubMessageRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun listUsers(): List<AdminUserResponse> {
        val staffByUserId = clubStaffRepository.findAllWithClub()
            .groupBy { it.id.userId }

        return userRepository.findAll().map { user ->
            val uid = user.id!!
            val clubRoles = staffByUserId[uid]?.map { cs ->
                UserClubRoleInfo(cs.club.id!!, cs.club.name, cs.role.name)
            } ?: emptyList()

            user.toDto(
                bookingsCount     = bookingRepository.countByUserId(uid),
                purchasesCount    = purchaseRepository.countByUserId(uid),
                totalSpentRub     = purchaseRepository.sumPaidByUserId(uid),
                visitedClubsCount = purchaseRepository.countDistinctClubsByUserId(uid),
                lastActivityAt    = listOfNotNull(
                    bookingRepository.findLatestCreatedAtByUserId(uid),
                    purchaseRepository.findLatestCreatedAtByUserId(uid)
                ).maxOrNull(),
                clubRoles         = clubRoles
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserDetails(userId: Long): AdminUserDetailsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        val uid = user.id!!
        val now = LocalDateTime.now()

        val bookingsCount     = bookingRepository.countByUserId(uid)
        val purchasesCount    = purchaseRepository.countByUserId(uid)
        val totalSpentRub     = purchaseRepository.sumPaidByUserId(uid)
        val visitedClubsCount = purchaseRepository.countDistinctClubsByUserId(uid)
        val lastActivityAt    = listOfNotNull(
            bookingRepository.findLatestCreatedAtByUserId(uid),
            purchaseRepository.findLatestCreatedAtByUserId(uid)
        ).maxOrNull()

        val recentPurchases = purchaseRepository
            .findRecentByUserIdFetch(uid, PageRequest.of(0, 10))
            .map { p ->
                UserPurchasePreview(
                    purchaseId    = p.id!!,
                    clubId        = p.club.id!!,
                    clubName      = p.club.name,
                    totalRub      = p.totalRub,
                    paymentStatus = p.paymentStatus.name,
                    createdAt     = p.createdAt
                )
            }

        val activeBlocks = clubUserBlockRepository
            .findActiveBlocksForUserFetch(uid, now)
            .map { b ->
                UserActiveBlockInfo(
                    clubId       = b.club.id!!,
                    clubName     = b.club.name,
                    reason       = b.reason,
                    blockedUntil = b.blockedUntil,
                    createdAt    = b.createdAt
                )
            }

        val recentBookings = bookingRepository
            .findRecentByUserId(uid, PageRequest.of(0, 10))
            .map { b ->
                UserBookingPreview(
                    bookingId = b.id!!,
                    clubId    = b.club.id!!,
                    clubName  = b.club.name,
                    startAt   = b.startAt,
                    endAt     = b.endAt,
                    status    = b.status.name,
                    totalRub  = b.totalRubSnapshot
                )
            }

        val clubRoles = clubStaffRepository
            .findAllByIdUserId(uid)
            .map { cs ->
                UserClubRoleInfo(
                    clubId   = cs.club.id!!,
                    clubName = cs.club.name,
                    role     = cs.role.name
                )
            }

        return AdminUserDetailsResponse(
            id                = uid,
            phone             = user.phone,
            isActive          = user.isActive,
            globalRole        = user.globalRole.name,
            hasPassword       = user.passwordHash != null,
            createdAt         = user.createdAt,
            updatedAt         = user.updatedAt,
            bookingsCount     = bookingsCount,
            purchasesCount    = purchasesCount,
            totalSpentRub     = totalSpentRub,
            visitedClubsCount = visitedClubsCount,
            lastActivityAt    = lastActivityAt,
            recentPurchases   = recentPurchases,
            activeBlocks      = activeBlocks,
            recentBookings    = recentBookings,
            clubRoles         = clubRoles
        )
    }

    @Transactional
    fun createUser(req: CreateUserRequest): AdminUserResponse {
        if (userRepository.findByPhone(req.phone).isPresent) {
            throw IllegalStateException("Пользователь с таким номером уже существует")
        }
        val role = runCatching { GlobalRole.valueOf(req.globalRole) }.getOrElse { throw IllegalArgumentException("Unknown role") }
        val user = userRepository.save(
            UserEntity(
                phone = req.phone,
                passwordHash = passwordEncoder.encode(req.password),
                globalRole = role
            )
        )
        return user.toDto()
    }

    @Transactional
    fun toggleUserActive(userId: Long, isActive: Boolean) {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        user.isActive = isActive
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    @Transactional
    fun deleteUser(userId: Long, currentUserId: Long) {
        if (userId == currentUserId) throw IllegalStateException("Cannot delete yourself")
        userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        userRepository.deleteById(userId)
    }

    private fun UserEntity.toDto(
        bookingsCount: Long = 0,
        purchasesCount: Long = 0,
        totalSpentRub: Long = 0,
        visitedClubsCount: Long = 0,
        lastActivityAt: LocalDateTime? = null,
        clubRoles: List<UserClubRoleInfo> = emptyList()
    ) = AdminUserResponse(
        id                = id!!,
        phone             = phone,
        isActive          = isActive,
        globalRole        = globalRole.name,
        hasPassword       = passwordHash != null,
        createdAt         = createdAt,
        updatedAt         = updatedAt,
        bookingsCount     = bookingsCount,
        purchasesCount    = purchasesCount,
        totalSpentRub     = totalSpentRub,
        visitedClubsCount = visitedClubsCount,
        lastActivityAt    = lastActivityAt,
        clubRoles         = clubRoles
    )

    @Transactional(readOnly = true)
    fun getUserAllReports(userId: Long): List<GlobalAdminUserReportItem> {
        userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return clubMessageRepository.findAllReportsByUserIdFetch(userId).map { r ->
            GlobalAdminUserReportItem(
                reportId  = r.id!!,
                clubId    = r.club.id!!,
                clubName  = r.club.name,
                message   = r.message,
                status    = r.status!!.name,
                createdAt = r.createdAt
            )
        }
    }

    @Transactional
    fun setGlobalRole(userId: Long, role: String) {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val gr = runCatching { GlobalRole.valueOf(role) }.getOrElse { throw IllegalArgumentException("Unknown role") }
        user.globalRole = gr
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getUserAllBookings(userId: Long): List<GlobalAdminUserBookingItem> {
        userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return bookingRepository.findAllByUserIdFetch(userId).map { b ->
            GlobalAdminUserBookingItem(
                bookingId  = b.id!!,
                clubId     = b.club.id!!,
                clubName   = b.club.name,
                startAt    = b.startAt,
                endAt      = b.endAt,
                status     = b.status.name,
                totalRub   = b.totalRubSnapshot,
                seatLabels = b.seats.map { it.seat.label },
                purchaseId = b.purchase?.id
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserAllPurchases(userId: Long): List<GlobalAdminUserPurchaseItem> {
        userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        return purchaseRepository.findAllByUserIdFetch(userId).map { p ->
            GlobalAdminUserPurchaseItem(
                purchaseId       = p.id!!,
                clubId           = p.club.id!!,
                clubName         = p.club.name,
                paymentStatus    = p.paymentStatus.name,
                totalRub         = p.totalRub,
                bookingTotalRub  = p.bookingTotalRub,
                productsTotalRub = p.productsTotalRub,
                createdAt        = p.createdAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPurchaseDetail(purchaseId: Long): AdminPurchaseDetailResponse {
        val purchase = purchaseRepository.findByIdFetch(purchaseId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found")

        val bookings = bookingRepository.findByPurchaseIds(listOf(purchaseId))
        val items    = productOrderItemRepository.findByPurchaseIds(listOf(purchaseId))

        val booking = bookings.firstOrNull()?.let { b ->
            val minutes = ChronoUnit.MINUTES.between(b.startAt, b.endAt)
            AdminPurchaseBookingDetail(
                bookingId     = b.id!!,
                status        = b.status,
                startAt       = isoFmt.format(b.startAt),
                endAt         = isoFmt.format(b.endAt),
                durationHours = minutes / 60.0,
                rateRubPerHour = b.rateRubPerHourSnapshot,
                totalRub      = b.totalRubSnapshot,
                seats         = b.seats.map { bs -> AdminPurchaseSeatDetail(bs.seat.id!!, bs.seat.label, bs.seat.type) }
            )
        }

        val productOrder = if (items.isNotEmpty()) {
            AdminPurchaseProductOrderDetail(
                orderId  = items.first().productOrderIdSnapshot ?: purchase.id!!,
                totalRub = purchase.productsTotalRub,
                items    = items.map { i ->
                    AdminPurchaseOrderItemDetail(
                        title       = i.titleSnapshot,
                        qty         = i.qty,
                        priceRub    = i.priceRubSnapshot,
                        subtotalRub = i.qty * i.priceRubSnapshot
                    )
                }
            )
        } else null

        return AdminPurchaseDetailResponse(
            id               = purchase.id!!,
            userId           = purchase.user.id!!,
            userPhone        = purchase.user.phone,
            clubId           = purchase.club.id!!,
            createdAt        = isoFmt.format(purchase.createdAt),
            paymentStatus    = purchase.paymentStatus,
            bookingTotalRub  = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub         = purchase.totalRub,
            booking          = booking,
            productOrder     = productOrder
        )
    }

    /** Глобальный админ может назначить/сменить главу клуба. */
    @Transactional
    fun setClubOwner(clubId: Long, userId: Long) {
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }

        // демотим предыдущих OWNER (если были)
        clubStaffRepository.findAllByIdClubId(clubId).forEach {
            if (it.role == ClubRole.OWNER) {
                it.role = ClubRole.ADMIN
                it.updatedAt = LocalDateTime.now()
                clubStaffRepository.save(it)
            }
        }

        val existing = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId).orElse(null)
        if (existing == null) {
            clubStaffRepository.save(
                ClubStaffEntity(
                    id = ClubStaffId(clubId = clubId, userId = userId),
                    club = club,
                    user = user,
                    role = ClubRole.OWNER,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        } else {
            existing.role = ClubRole.OWNER
            existing.updatedAt = LocalDateTime.now()
            clubStaffRepository.save(existing)
        }
    }
}
