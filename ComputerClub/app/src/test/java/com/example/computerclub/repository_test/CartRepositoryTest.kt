package com.example.computerclub.repository_test

import com.example.computerclub.data.network.CartApi
import com.example.computerclub.data.network.dto.AddCartBookingRequestDto
import com.example.computerclub.data.network.dto.AddCartProductRequestDto
import com.example.computerclub.data.network.dto.CartResponseDto
import com.example.computerclub.data.network.dto.SelectClubRequestDto
import com.example.computerclub.data.network.dto.SetCartBookingSeatsRequestDto
import com.example.computerclub.data.network.dto.UpdateCartProductQtyRequestDto
import com.example.computerclub.data.repository.CartRepository
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CartRepositoryTest {

    @Test
    fun getOrCreateSelectsClubWhenCartDoesNotExist() = runBlocking {
        val api = FakeCartApi(cartNotFound = true)
        val result = CartRepository(api).getOrCreate(clubId = 7)

        assertEquals(7L, result.clubId)
        assertEquals(SelectClubRequestDto(clubId = 7), api.lastSelectClubRequest)
    }

    @Test
    fun addProductSendsClubProductAndQuantity() = runBlocking {
        val api = FakeCartApi()

        CartRepository(api).addProduct(clubId = 7, productId = 11, qty = 3)

        assertEquals(7L, api.lastAddProductClubId)
        assertEquals(AddCartProductRequestDto(productId = 11, qty = 3), api.lastAddProductRequest)
    }

    @Test
    fun setBookingSeatsSendsSelectedSeatIds() = runBlocking {
        val api = FakeCartApi()

        CartRepository(api).setBookingSeats(clubId = 7, lineId = 21, seatIds = listOf(1, 2, 3))

        assertEquals(7L, api.lastSetSeatsClubId)
        assertEquals(21L, api.lastSetSeatsLineId)
        assertEquals(SetCartBookingSeatsRequestDto(seatIds = listOf(1, 2, 3)), api.lastSetSeatsRequest)
    }

    @Test
    fun deleteBookingLineUsesBookingItemType() = runBlocking {
        val api = FakeCartApi()

        CartRepository(api).deleteBookingLine(clubId = 7, lineId = 21)

        assertEquals("booking", api.lastDeleteType)
        assertEquals(21L, api.lastDeleteId)
        assertEquals(7L, api.lastDeleteClubId)
        assertNull(api.lastSelectClubRequest)
    }

    private class FakeCartApi(private val cartNotFound: Boolean = false) : CartApi {
        var lastSelectClubRequest: SelectClubRequestDto? = null
        var lastAddProductClubId: Long? = null
        var lastAddProductRequest: AddCartProductRequestDto? = null
        var lastSetSeatsLineId: Long? = null
        var lastSetSeatsClubId: Long? = null
        var lastSetSeatsRequest: SetCartBookingSeatsRequestDto? = null
        var lastDeleteType: String? = null
        var lastDeleteId: Long? = null
        var lastDeleteClubId: Long? = null

        override suspend fun selectClub(dto: SelectClubRequestDto): CartResponseDto {
            lastSelectClubRequest = dto
            return cart(clubId = dto.clubId)
        }

        override suspend fun getCart(clubId: Long): CartResponseDto {
            if (cartNotFound) {
                throw HttpException(Response.error<String>(404, "".toResponseBody()))
            }
            return cart(clubId)
        }

        override suspend fun addProduct(clubId: Long, dto: AddCartProductRequestDto): CartResponseDto {
            lastAddProductClubId = clubId
            lastAddProductRequest = dto
            return cart(clubId)
        }

        override suspend fun addBooking(clubId: Long, dto: AddCartBookingRequestDto): CartResponseDto =
            cart(clubId)

        override suspend fun setBookingSeats(
            lineId: Long,
            clubId: Long,
            dto: SetCartBookingSeatsRequestDto
        ): CartResponseDto {
            lastSetSeatsLineId = lineId
            lastSetSeatsClubId = clubId
            lastSetSeatsRequest = dto
            return cart(clubId)
        }

        override suspend fun updateQty(lineId: Long, clubId: Long, dto: UpdateCartProductQtyRequestDto): CartResponseDto =
            cart(clubId)

        override suspend fun deleteItem(type: String, id: Long, clubId: Long): CartResponseDto {
            lastDeleteType = type
            lastDeleteId = id
            lastDeleteClubId = clubId
            return cart(clubId)
        }

        override suspend fun clear(clubId: Long) = Unit

        private fun cart(clubId: Long): CartResponseDto =
            CartResponseDto(
                cartId = 1,
                userId = 1,
                clubId = clubId,
                updatedAt = "2026-05-15T12:00:00"
            )
    }
}
