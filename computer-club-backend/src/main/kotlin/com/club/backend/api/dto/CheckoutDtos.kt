package com.club.backend.api.dto

import jakarta.validation.constraints.NotNull

data class CheckoutRequest(
    @field:NotNull val clubId: Long
)

data class CheckoutResponse(
    val purchaseId: Long,
    val paymentStatus: String,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int
)

data class PurchaseListItemResponse(
    val purchaseId: Long,
    val clubId: Long,
    val clubName: String,
    val createdAt: String,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int,
    val paymentStatus: String
)
