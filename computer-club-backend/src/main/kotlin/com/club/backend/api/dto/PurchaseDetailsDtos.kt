package com.club.backend.api.dto

data class PurchaseDetailsResponse(
    val purchaseId: Long,
    val clubId: Long,
    val clubName: String,
    val createdAt: String,
    val paymentStatus: String,
    val bookingItems: List<BookingItemResponse>,
    val productItems: List<ProductItemResponse>,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int
)

data class BookingItemResponse(
    val bookingId: Long,
    val startAt: String,
    val endAt: String,
    val seatIds: List<Long>,
    val seatLabels: List<String>,
    val totalRub: Int,
    val rateRubPerHourSnapshot: Int,
    val packageHours: Int?
)

data class ProductItemResponse(
    val productId: Long?,
    val name: String,
    val qty: Int,
    val unitRub: Int,
    val totalRub: Int
)