package com.club.backend.api.dto.admin

import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentMethod
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.domain.enum.ProductOrderStatus
import com.club.backend.domain.enum.SeatType
import java.time.LocalDateTime

data class AdminBookingResponse(
    val id: Long,
    val userId: Long,
    val clubId: Long,
    val userPhone: String?,
    val status: BookingStatus,
    val startAt: String,
    val endAt: String,
    val durationHours: Double,
    val totalRub: Int,
    val seatLabels: List<String>,
    val purchaseId: Long?
)

data class AdminBookingDetailResponse(
    val id: Long,
    val status: BookingStatus,
    val userId: Long,
    val userPhone: String?,
    val startAt: String,
    val endAt: String,
    val durationHours: Double,
    val rateRubPerHour: Int,
    val totalRub: Int,
    val seats: List<AdminPurchaseSeatDetail>,
    val purchaseId: Long?
)

data class AdminPurchaseResponse(
    val id: Long,
    val userId: Long,
    val clubId: Long,
    val userPhone: String?,
    val paymentStatus: PaymentStatus,
    val totalAmountRub: Int,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val seatLabels: List<String>,
    val productCount: Int,
    val createdAt: String
)

data class AdminPurchaseDetailResponse(
    val id: Long,
    val userId: Long,
    val userPhone: String?,
    val clubId: Long,
    val createdAt: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int,
    val booking: AdminPurchaseBookingDetail?,
    val productOrder: AdminPurchaseProductOrderDetail?
)

data class AdminPurchaseBookingDetail(
    val bookingId: Long,
    val status: BookingStatus,
    val startAt: String,
    val endAt: String,
    val durationHours: Double,
    val rateRubPerHour: Int,
    val totalRub: Int,
    val seats: List<AdminPurchaseSeatDetail>
)

data class AdminPurchaseSeatDetail(
    val seatId: Long,
    val label: String,
    val type: SeatType
)

data class AdminPurchaseProductOrderDetail(
    val orderId: Long,
    val status: ProductOrderStatus,
    val totalRub: Int,
    val items: List<AdminPurchaseOrderItemDetail>
)

data class AdminPurchaseOrderItemDetail(
    val title: String,
    val qty: Int,
    val priceRub: Int,
    val subtotalRub: Int
)

// preview-DTO для дашборда — только нужные поля, без балласта list-ответов
data class DashboardBookingPreview(
    val id: Long,
    val userPhone: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: BookingStatus
)

data class DashboardPurchasePreview(
    val id: Long,
    val userPhone: String?,
    val totalRub: Int,
    val paymentStatus: PaymentStatus,
    val createdAt: LocalDateTime
)

data class ClubDashboardResponse(
    val activeBookingsCount: Int,
    val upcomingTodayCount: Int,
    val occupiedSeats: Int,
    val totalSeats: Int,
    val todayRevenueRub: Long,
    /** null если у запрашивающего нет прав на расширенную финансовую статистику */
    val weekRevenueRub: Long?,
    val monthRevenueRub: Long?,
    val recentBookings: List<DashboardBookingPreview>,
    val recentPurchases: List<DashboardPurchasePreview>
)

// одна запись в снимке занятости зала: место + активная/предстоящая бронь
data class FloorplanBookingEntry(
    val seatId: Long,
    val bookingId: Long,
    val userId: Long,
    val userPhone: String?,
    val status: BookingStatus,
    val startAt: String,
    val endAt: String,
    val totalRub: Int,
    val paymentStatus: String? // null если бронь без заказа
)
