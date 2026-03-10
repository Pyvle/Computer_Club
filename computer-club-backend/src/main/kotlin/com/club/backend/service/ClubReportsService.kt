package com.club.backend.service

import com.club.backend.api.dto.admin.AdminBookingDetailResponse
import com.club.backend.api.dto.admin.AdminBookingResponse
import com.club.backend.api.dto.admin.AdminPurchaseBookingDetail
import com.club.backend.api.dto.admin.AdminPurchaseDetailResponse
import com.club.backend.api.dto.admin.AdminPurchaseOrderItemDetail
import com.club.backend.api.dto.admin.AdminPurchaseProductOrderDetail
import com.club.backend.api.dto.admin.AdminPurchaseResponse
import com.club.backend.api.dto.admin.AdminPurchaseSeatDetail
import com.club.backend.domain.entity.BookingEntity
import com.club.backend.domain.entity.PurchaseEntity
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.repository.BookingRepository
import com.club.backend.repository.ProductOrderItemRepository
import com.club.backend.repository.PurchaseRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class ClubReportsService(
    private val bookingRepository: BookingRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productOrderItemRepository: ProductOrderItemRepository
) {

    // ISO formatter — тот же формат что и в ClubAdminManagementService
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @Transactional(readOnly = true)
    fun bookings(clubId: Long, from: LocalDateTime?, to: LocalDateTime?, status: BookingStatus?): List<AdminBookingResponse> =
        bookingRepository.findForAdmin(clubId, from, to, status).map { it.toDto() }

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
        val purchases = purchaseRepository.findForAdmin(clubId, from, to, status)
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
