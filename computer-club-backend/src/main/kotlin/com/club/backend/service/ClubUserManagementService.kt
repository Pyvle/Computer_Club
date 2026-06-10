package com.club.backend.service

import com.club.backend.api.dto.admin.*
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class ClubUserManagementService(
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val purchaseRepository: PurchaseRepository,
    private val clubUserBlockRepository: ClubUserBlockRepository,
    private val clubMessageRepository: ClubMessageRepository
) {

    @Transactional(readOnly = true)
    fun listClubUsers(clubId: Long): List<ClubUserListItem> {
        val bookings  = bookingRepository.findAllForAdmin(clubId)
        val purchases = purchaseRepository.findAllForAdmin(clubId)
        val blockMap  = clubUserBlockRepository.findAllByClubIdFetched(clubId)
            .associateBy { it.id.userId }

        // Собираем уникальных пользователей
        data class BookingAgg(
            var count: Long = 0,
            var cancelled: Long = 0,
            var firstVisit: LocalDateTime? = null,
            var lastVisit: LocalDateTime? = null
        )
        data class PurchaseAgg(var count: Long = 0, var totalSpent: Long = 0)

        val bookingAgg  = mutableMapOf<Long, BookingAgg>()
        val purchaseAgg = mutableMapOf<Long, PurchaseAgg>()
        val userInfo    = mutableMapOf<Long, Pair<String?, Boolean>>()

        bookings.forEach { b ->
            val uid = b.user.id!!
            userInfo.putIfAbsent(uid, Pair(b.user.phone, b.user.isActive))
            val agg = bookingAgg.getOrPut(uid) { BookingAgg() }
            agg.count++
            if (b.status == BookingStatus.CANCELED) agg.cancelled++
            val start = b.startAt
            if (agg.firstVisit == null || start.isBefore(agg.firstVisit)) agg.firstVisit = start
            if (agg.lastVisit == null || start.isAfter(agg.lastVisit))    agg.lastVisit  = start
        }

        purchases.forEach { p ->
            val uid = p.user.id!!
            userInfo.putIfAbsent(uid, Pair(p.user.phone, p.user.isActive))
            val agg = purchaseAgg.getOrPut(uid) { PurchaseAgg() }
            agg.count++
            if (p.paymentStatus == PaymentStatus.PAID) agg.totalSpent += p.totalRub
        }

        val now = LocalDateTime.now()
        return userInfo.map { (uid, info) ->
            val (phone, isActive) = info
            val bAgg  = bookingAgg[uid]  ?: BookingAgg()
            val pAgg  = purchaseAgg[uid] ?: PurchaseAgg()
            val block = blockMap[uid]
            val blockUntil = block?.blockedUntil
            val effectivelyBlocked = block != null && block.isBlocked &&
                (blockUntil == null || blockUntil.isAfter(now))

            ClubUserListItem(
                userId                = uid,
                phone                 = phone,
                isActive              = isActive,
                firstVisitAt          = bAgg.firstVisit,
                lastVisitAt           = bAgg.lastVisit,
                bookingsCount         = bAgg.count,
                purchasesCount        = pAgg.count,
                totalSpentRub         = pAgg.totalSpent,
                cancelledBookingsCount = bAgg.cancelled,
                isBlocked             = effectivelyBlocked,
                blockedUntil          = if (effectivelyBlocked) blockUntil else null,
                blockReason           = if (effectivelyBlocked) block?.reason else null
            )
        }.sortedByDescending { it.lastVisitAt }
    }

    @Transactional(readOnly = true)
    fun getClubUserDetail(clubId: Long, userId: Long): ClubUserDetailResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        val bookings  = bookingRepository.findAllByClubIdAndUserIdFetch(clubId, userId)
        val purchases = purchaseRepository.findAllByClubIdAndUserIdFetch(clubId, userId)
        val block     = clubUserBlockRepository.findByClubIdAndUserIdFetch(clubId, userId)
        val reports   = clubMessageRepository.findAllReportsByClubIdAndUserIdFetch(clubId, userId)

        val now = LocalDateTime.now()
        val blockUntil = block?.blockedUntil
        val effectivelyBlocked = block != null && block.isBlocked &&
            (blockUntil == null || blockUntil.isAfter(now))

        // Метрики по бронированиям
        val nonCancelled = bookings.filter { it.status != BookingStatus.CANCELED }
        val totalHoursBooked = nonCancelled.sumOf { b ->
            Duration.between(b.startAt, b.endAt).toMinutes() / 60.0
        }
        val cancelledCount = bookings.count { it.status == BookingStatus.CANCELED }.toLong()

        val seatTypeCounts = bookings
            .flatMap { b -> b.seats.map { it.seat.type.name } }
            .groupingBy { it }
            .eachCount()
        val favoriteSeatType = seatTypeCounts.maxByOrNull { it.value }?.key

        val firstVisitAt = bookings.minOfOrNull { it.startAt }
        val lastVisitAt  = bookings.maxOfOrNull { it.startAt }

        // Метрики по покупкам
        val paidPurchases = purchases.filter { it.paymentStatus == PaymentStatus.PAID }
        val totalSpentRub = paidPurchases.sumOf { it.totalRub.toLong() }
        val avgSpentRub   = if (paidPurchases.isNotEmpty()) totalSpentRub / paidPurchases.size else 0L

        val recentBookings = bookings.take(20).map { b ->
            ClubUserBookingItem(
                bookingId    = b.id!!,
                startAt      = b.startAt,
                endAt        = b.endAt,
                seatLabels   = b.seats.map { it.seat.label },
                durationHours = Duration.between(b.startAt, b.endAt).toMinutes() / 60.0,
                totalRub     = b.totalRubSnapshot,
                status       = b.status.name
            )
        }

        val recentPurchases = purchases.take(20).map { p ->
            ClubUserPurchaseItem(
                purchaseId    = p.id!!,
                createdAt     = p.createdAt,
                totalRub      = p.totalRub,
                paymentStatus = p.paymentStatus.name
            )
        }

        val reportItems = reports.map { r ->
            ClubUserReportForDetail(
                reportId  = r.id!!,
                message   = r.message,
                status    = r.status!!.name,
                createdAt = r.createdAt
            )
        }

        return ClubUserDetailResponse(
            userId                = userId,
            phone                 = user.phone,
            isActive              = user.isActive,
            isBlocked             = effectivelyBlocked,
            blockReason           = if (effectivelyBlocked) block?.reason else null,
            blockedUntil          = if (effectivelyBlocked) blockUntil else null,
            blockedAt             = if (effectivelyBlocked) block?.updatedAt else null,
            blockedByPhone        = if (effectivelyBlocked) block?.blockedBy?.phone else null,
            firstVisitAt          = firstVisitAt,
            lastVisitAt           = lastVisitAt,
            bookingsCount         = bookings.size.toLong(),
            purchasesCount        = purchases.size.toLong(),
            totalSpentRub         = totalSpentRub,
            avgSpentRub           = avgSpentRub,
            cancelledBookingsCount = cancelledCount,
            totalHoursBooked      = totalHoursBooked,
            favoriteSeatType      = favoriteSeatType,
            recentBookings        = recentBookings,
            recentPurchases       = recentPurchases,
            reports               = reportItems
        )
    }
}
