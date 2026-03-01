package com.club.backend.api.dto

import com.club.backend.domain.enum.PaymentMethod
import jakarta.validation.constraints.NotNull

data class CheckoutRequest(
    @field:NotNull val clubId: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD
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
    val createdAt: String,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int,
    val paymentStatus: String
)
