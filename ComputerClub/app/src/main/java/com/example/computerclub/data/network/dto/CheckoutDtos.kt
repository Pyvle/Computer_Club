package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequestDto(
    val clubId: Long
)

@Serializable
data class CheckoutResponseDto(
    val purchaseId: Long,
    val paymentStatus: String,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int
)

@Serializable
data class PurchaseListItemDto(
    val purchaseId: Long,
    val clubId: Long,
    val createdAt: String,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int,
    val paymentStatus: String
)

@Serializable
data class BookingItemDto(
    val bookingId: Long,
    val startAt: String,
    val endAt: String,
    val seatIds: List<Long>,
    val seatLabels: List<String>,
    val totalRub: Int
)

@Serializable
data class ProductItemDto(
    val productId: Long,
    val name: String,
    val qty: Int,
    val unitRub: Int,
    val totalRub: Int
)

@Serializable
data class PurchaseDetailsDto(
    val purchaseId: Long,
    val clubId: Long,
    val createdAt: String,
    val paymentStatus: String,
    val bookingItems: List<BookingItemDto>,
    val productItems: List<ProductItemDto>,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int
)
