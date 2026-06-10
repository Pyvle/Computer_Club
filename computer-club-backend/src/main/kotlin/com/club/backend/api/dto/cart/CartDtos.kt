package com.club.backend.api.dto.cart

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class SelectClubRequest(
    @field:NotNull val clubId: Long
)

data class AddCartBookingRequest(
    @field:NotNull val startAt: LocalDateTime,
    @field:NotNull val endAt: LocalDateTime,
    val packageHours: Int? = null
)

data class SetCartBookingSeatsRequest(
    val seatIds: List<Long>
)

data class AddCartProductRequest(
    @field:NotNull val productId: Long,
    @field:Min(1) val qty: Int
)

data class UpdateCartProductQtyRequest(
    @field:Min(0) val qty: Int
)

data class CartResponse(
    val cartId: Long,
    val userId: Long,
    val clubId: Long,
    val updatedAt: String,
    val bookings: List<CartBookingLineResponse>,
    val products: List<CartProductLineResponse>
)

data class CartBookingLineResponse(
    val lineId: Long,
    val startAt: String,
    val endAt: String,
    val packageHours: Int?,
    val seatIds: List<Long>,
    val lineTotalRub: Int
)

data class CartProductLineResponse(
    val lineId: Long,
    val productId: Long,
    val title: String,
    val qty: Int,
    val priceRub: Int,
    val lineTotalRub: Int
)
