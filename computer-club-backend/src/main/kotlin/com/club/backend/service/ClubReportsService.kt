package com.club.backend.service

import com.club.backend.api.dto.admin.AdminBookingDetailResponse
import com.club.backend.api.dto.admin.AdminBookingResponse
import com.club.backend.api.dto.admin.AdminPurchaseBookingDetail
import com.club.backend.api.dto.admin.AdminPurchaseDetailResponse
import com.club.backend.api.dto.admin.AdminPurchaseOrderItemDetail
import com.club.backend.api.dto.admin.AdminPurchaseProductOrderDetail
import com.club.backend.api.dto.admin.AdminPurchaseResponse
import com.club.backend.api.dto.admin.AdminPurchaseSeatDetail
import com.club.backend.api.dto.admin.ClubDashboardResponse
import com.club.backend.api.dto.admin.FloorplanBookingEntry
import com.club.backend.domain.entity.BookingEntity
import com.club.backend.domain.entity.PurchaseEntity
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.repository.BookingRepository
import com.club.backend.repository.ProductOrderItemRepository
import com.club.backend.repository.PurchaseRepository
import com.club.backend.repository.SeatRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class ClubReportsService(
    private val bookingRepository: BookingRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productOrderItemRepository: ProductOrderItemRepository,
    private val seatRepository: SeatRepository
) {

    // ISO formatter — тот же формат что и в ClubAdminManagementService
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @Transactional(readOnly = true)
    fun bookings(clubId: Long, from: LocalDateTime?, to: LocalDateTime?, status: BookingStatus?): List<AdminBookingResponse> =
        bookingRepository.findAllForAdmin(clubId)
            .filter { from == null || !it.startAt.isBefore(from) }
            .filter { to == null || !it.startAt.isAfter(to) }
            .filter { status == null || it.status == status }
            .map { it.toDto() }

    @Transactional(readOnly = true)
    fun bookingDetail(clubId: Long, bookingId: Long): AdminBookingDetailResponse {
        val b = bookingRepository.findByIdAndClubIdFetch(bookingId, clubId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")
        val minutes = ChronoUnit.MINUTES.between(b.startAt, b.endAt)
        return AdminBookingDetailResponse(
            id = b.id!!,
            status = b.status,
            userId = b.user.id!!,
            userPhone = b.user.phone,
            startAt = isoFmt.format(b.startAt),
            endAt = isoFmt.format(b.endAt),
            durationHours = minutes / 60.0,
            rateRubPerHour = b.rateRubPerHourSnapshot,
            totalRub = b.totalRubSnapshot,
            seats = b.seats.map { bs -> AdminPurchaseSeatDetail(bs.seat.id!!, bs.seat.label, bs.seat.type) },
            purchaseId = b.purchase?.id
        )
    }

    fun purchases(clubId: Long, from: LocalDateTime?, to: LocalDateTime?, status: PaymentStatus?): List<AdminPurchaseResponse> {
        val purchases = purchaseRepository.findAllForAdmin(clubId)
            .filter { from == null || !it.createdAt.isBefore(from) }
            .filter { to == null || !it.createdAt.isAfter(to) }
            .filter { status == null || it.paymentStatus == status }
        if (purchases.isEmpty()) return emptyList()
        val ids = purchases.map { it.id!! }
        val bookingsByPurchase = bookingRepository.findByPurchaseIds(ids)
            .groupBy { it.purchase!!.id!! }
        val itemsByPurchase = productOrderItemRepository.findByPurchaseIds(ids)
            .groupBy { it.productOrder.purchase.id!! }
        return purchases.map { p ->
            val pid = p.id!!
            AdminPurchaseResponse(
                id = pid,
                userId = p.user.id!!,
                clubId = p.club.id!!,
                userPhone = p.user.phone,
                paymentStatus = p.paymentStatus,
                totalAmountRub = p.totalRub,
                bookingTotalRub = p.bookingTotalRub,
                productsTotalRub = p.productsTotalRub,
                seatLabels = (bookingsByPurchase[pid] ?: emptyList())
                    .flatMap { b -> b.seats.map { it.seat.label } },
                productCount = (itemsByPurchase[pid] ?: emptyList()).sumOf { it.qty },
                createdAt = p.createdAt.toString()
            )
        }
    }

    @Transactional
    fun adminCancel(clubId: Long, purchaseId: Long): AdminPurchaseResponse {
        val purchase = purchaseRepository.findById(purchaseId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found")
        }
        if (purchase.club.id != clubId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Purchase does not belong to this club")
        }
        if (purchase.paymentStatus == PaymentStatus.CANCELED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already cancelled")
        }
        purchase.paymentStatus = PaymentStatus.CANCELED
        purchaseRepository.save(purchase)

        // отменяем все UPCOMING/ACTIVE брони заказа одним запросом
        bookingRepository.cancelByPurchaseId(purchase.id!!)

        return purchase.toDtoSimple()
    }

    @Transactional(readOnly = true)
    fun purchaseDetail(clubId: Long, purchaseId: Long): AdminPurchaseDetailResponse {
        // clubId фильтруется прямо в запросе — один NOT_FOUND вместо NOT_FOUND + FORBIDDEN
        val purchase = purchaseRepository.findByIdAndClubIdFetch(purchaseId, clubId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found")

        val bookings = bookingRepository.findByPurchaseIds(listOf(purchaseId))
        val items = productOrderItemRepository.findByPurchaseIds(listOf(purchaseId))

        val booking = bookings.firstOrNull()?.let { b ->
            val minutes = ChronoUnit.MINUTES.between(b.startAt, b.endAt)
            AdminPurchaseBookingDetail(
                bookingId = b.id!!,
                status = b.status,
                startAt = isoFmt.format(b.startAt),
                endAt = isoFmt.format(b.endAt),
                durationHours = minutes / 60.0,
                rateRubPerHour = b.rateRubPerHourSnapshot,
                totalRub = b.totalRubSnapshot,
                seats = b.seats.map { bs ->
                    AdminPurchaseSeatDetail(bs.seat.id!!, bs.seat.label, bs.seat.type)
                }
            )
        }

        val productOrder = items.firstOrNull()?.productOrder?.let { po ->
            AdminPurchaseProductOrderDetail(
                orderId = po.id!!,
                status = po.status,
                totalRub = po.totalRubSnapshot,
                items = items.map { i ->
                    AdminPurchaseOrderItemDetail(
                        title = i.titleSnapshot,
                        qty = i.qty,
                        priceRub = i.priceRubSnapshot,
                        subtotalRub = i.qty * i.priceRubSnapshot
                    )
                }
            )
        }

        return AdminPurchaseDetailResponse(
            id = purchase.id!!,
            userId = purchase.user.id!!,
            userPhone = purchase.user.phone,
            clubId = purchase.club.id!!,
            createdAt = isoFmt.format(purchase.createdAt),
            paymentMethod = purchase.paymentMethod,
            paymentStatus = purchase.paymentStatus,
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub,
            booking = booking,
            productOrder = productOrder
        )
    }

    /** Снимок занятости зала: брони, активные в момент [at]. */
    @Transactional(readOnly = true)
    fun floorplanBookings(clubId: Long, at: LocalDateTime): List<FloorplanBookingEntry> =
        bookingRepository.findAtMoment(clubId, at).flatMap { b ->
            b.seats.map { bs ->
                FloorplanBookingEntry(
                    seatId = bs.seat.id!!,
                    bookingId = b.id!!,
                    userId = b.user.id!!,
                    userPhone = b.user.phone,
                    status = b.status,
                    startAt = isoFmt.format(b.startAt),
                    endAt = isoFmt.format(b.endAt),
                    totalRub = b.totalRubSnapshot,
                    paymentStatus = b.purchase?.paymentStatus?.name
                )
            }
        }

    @Transactional(readOnly = true)
    fun dashboard(clubId: Long, includeExtendedRevenue: Boolean): ClubDashboardResponse {
        val todayStart    = LocalDate.now().atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)

        val weekStart  = todayStart.minusDays(6)   // скользящие 7 дней
        val monthStart = todayStart.minusDays(29)  // скользящие 30 дней

        return ClubDashboardResponse(
            activeBookingsCount = bookingRepository.countByClubIdAndStatus(clubId, BookingStatus.ACTIVE).toInt(),
            upcomingTodayCount  = bookingRepository.countUpcomingToday(clubId, todayStart, tomorrowStart).toInt(),
            occupiedSeats       = bookingRepository.countOccupiedSeats(clubId).toInt(),
            totalSeats          = seatRepository.countByClubIdAndIsActiveTrue(clubId).toInt(),
            todayRevenueRub     = purchaseRepository.sumPaidRevenue(clubId, todayStart, tomorrowStart),
            weekRevenueRub      = if (includeExtendedRevenue) purchaseRepository.sumPaidRevenue(clubId, weekStart, tomorrowStart) else null,
            monthRevenueRub     = if (includeExtendedRevenue) purchaseRepository.sumPaidRevenue(clubId, monthStart, tomorrowStart) else null,
            recentBookings      = bookingRepository.findRecentPreviews(clubId, PageRequest.of(0, 5)),
            recentPurchases     = purchaseRepository.findRecentPreviews(clubId, PageRequest.of(0, 5))
        )
    }

    private fun BookingEntity.toDto(): AdminBookingResponse {
        val minutes = ChronoUnit.MINUTES.between(startAt, endAt)
        return AdminBookingResponse(
            id = requireNotNull(id),
            userId = user.id!!,
            clubId = club.id!!,
            userPhone = user.phone,
            status = status,
            startAt = isoFmt.format(startAt),
            endAt = isoFmt.format(endAt),
            durationHours = minutes / 60.0,
            totalRub = totalRubSnapshot,
            seatLabels = seats.map { it.seat.label },
            purchaseId = purchase?.id
        )
    }

    private fun PurchaseEntity.toDtoSimple() = AdminPurchaseResponse(
        id = requireNotNull(id),
        userId = user.id!!,
        clubId = club.id!!,
        userPhone = user.phone,
        paymentStatus = paymentStatus,
        totalAmountRub = totalRub,
        bookingTotalRub = bookingTotalRub,
        productsTotalRub = productsTotalRub,
        seatLabels = emptyList(),
        productCount = 0,
        createdAt = createdAt.toString()
    )
}
