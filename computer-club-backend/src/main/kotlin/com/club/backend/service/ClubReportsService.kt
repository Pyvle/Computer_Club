package com.club.backend.service

import com.club.backend.api.dto.admin.AdminBookingResponse
import com.club.backend.api.dto.admin.AdminPurchaseResponse
import com.club.backend.domain.entity.BookingEntity
import com.club.backend.domain.entity.PurchaseEntity
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.repository.BookingRepository
import com.club.backend.repository.PurchaseRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClubReportsService(
    private val bookingRepository: BookingRepository,
    private val purchaseRepository: PurchaseRepository
) {

    fun bookings(clubId: Long, from: LocalDateTime?, to: LocalDateTime?, status: BookingStatus?): List<AdminBookingResponse> =
        bookingRepository.findForAdmin(clubId, from, to, status).map { it.toDto() }

    fun purchases(clubId: Long, from: LocalDateTime?, to: LocalDateTime?, status: PaymentStatus?): List<AdminPurchaseResponse> =
        purchaseRepository.findForAdmin(clubId, from, to, status).map { it.toDto() }

    private fun BookingEntity.toDto() = AdminBookingResponse(
        id = requireNotNull(id),
        userId = user.id!!,
        clubId = club.id!!,
        status = status,
        startAt = startAt.toString(),
        endAt = endAt.toString(),
        seatIds = seats.map { it.seat.id!! }
    )

    private fun PurchaseEntity.toDto() = AdminPurchaseResponse(
        id = requireNotNull(id),
        userId = user.id!!,
        clubId = club.id!!,
        paymentStatus = paymentStatus,
        totalAmountRub = totalRub,
        createdAt = createdAt.toString()
    )
}
