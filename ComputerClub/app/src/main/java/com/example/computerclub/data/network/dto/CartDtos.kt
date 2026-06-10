package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SelectClubRequestDto(
    val clubId: Long
)

@Serializable
data class AddCartProductRequestDto(
    val productId: Long,
    val qty: Int
)

@Serializable
data class AddCartBookingRequestDto(
    val startAt: String,
    val endAt: String,
    val packageHours: Int? = null
)

@Serializable
data class SetCartBookingSeatsRequestDto(
    val seatIds: List<Long>
)

@Serializable
data class UpdateCartProductQtyRequestDto(
    val qty: Int
)

@Serializable
data class CartResponseDto(
    val cartId: Long,
    val userId: Long,
    val clubId: Long,
    val updatedAt: String,
    val bookings: List<CartBookingLineResponseDto> = emptyList(),
    val products: List<CartProductLineResponseDto> = emptyList()
)

@Serializable
data class CartBookingLineResponseDto(
    val lineId: Long,
    val startAt: String,
    val endAt: String,
    val packageHours: Int? = null,
    val seatIds: List<Long> = emptyList(),
    val lineTotalRub: Int? = null
)

@Serializable
data class CartProductLineResponseDto(
    val lineId: Long,
    val productId: Long,
    val title: String,
    val qty: Int,
    val priceRub: Int,
    val lineTotalRub: Int
)
