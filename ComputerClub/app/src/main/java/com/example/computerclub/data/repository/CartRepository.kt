package com.example.computerclub.data.repository

import com.example.computerclub.data.network.CartApi
import com.example.computerclub.data.network.dto.*
import retrofit2.HttpException

class CartRepository(private val api: CartApi) {

    suspend fun getOrCreate(clubId: Long): CartResponseDto {
        return try {
            api.getCart(clubId)
        } catch (e: HttpException) {
            if (e.code() == 404) api.selectClub(SelectClubRequestDto(clubId)) else throw e
        }
    }

    suspend fun addProduct(clubId: Long, productId: Long, qty: Int): CartResponseDto =
        api.addProduct(clubId, AddCartProductRequestDto(productId, qty))

    suspend fun addBooking(
        clubId: Long,
        startAtIso: String,
        endAtIso: String,
        packageHours: Int?
    ): CartResponseDto = api.addBooking(clubId, AddCartBookingRequestDto(startAtIso, endAtIso, packageHours))

    suspend fun setBookingSeats(clubId: Long, lineId: Long, seatIds: List<Long>): CartResponseDto =
        api.setBookingSeats(lineId = lineId, clubId = clubId, dto = SetCartBookingSeatsRequestDto(seatIds))

    suspend fun updateQty(clubId: Long, lineId: Long, qty: Int): CartResponseDto =
        api.updateQty(lineId, clubId, UpdateCartProductQtyRequestDto(qty))

    suspend fun deleteProductLine(clubId: Long, lineId: Long): CartResponseDto =
        api.deleteItem(clubId, type = "product", id = lineId)

    suspend fun deleteBookingLine(clubId: Long, lineId: Long): CartResponseDto =
        api.deleteItem(clubId, type = "booking", id = lineId)

    suspend fun clear(clubId: Long) = api.clear(clubId)
}
