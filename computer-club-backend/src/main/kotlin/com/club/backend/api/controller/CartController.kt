package com.club.backend.api.controller

import com.club.backend.api.dto.cart.*
import com.club.backend.service.CartService
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cart")
class CartController(
    private val cartService: CartService
) {
    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized")
        return principal.toLong()
    }

    @PutMapping("/club")
    fun selectClub(
        @Valid @RequestBody request: SelectClubRequest
    ): CartResponse =
        cartService.selectClub(currentUserId(), request.clubId)

    @GetMapping
    fun getCart(
        @RequestParam clubId: Long
    ): CartResponse =
        cartService.getCart(currentUserId(), clubId)

    @PostMapping("/bookings")
    fun addBooking(
        @RequestParam clubId: Long,
        @Valid @RequestBody request: AddCartBookingRequest
    ): CartResponse =
        cartService.addBooking(currentUserId(), clubId, request)

    @PostMapping("/bookings/{lineId}/seats")
    fun setBookingSeats(
        @RequestParam clubId: Long,
        @PathVariable lineId: Long,
        @Valid @RequestBody request: SetCartBookingSeatsRequest
    ): CartResponse =
        cartService.setBookingSeats(currentUserId(), clubId, lineId, request)

    @PostMapping("/products")
    fun addProduct(
        @RequestParam clubId: Long,
        @Valid @RequestBody request: AddCartProductRequest
    ): CartResponse =
        cartService.addProduct(currentUserId(), clubId, request)

    @PatchMapping("/products/{lineId}")
    fun updateProductQty(
        @RequestParam clubId: Long,
        @PathVariable lineId: Long,
        @Valid @RequestBody request: UpdateCartProductQtyRequest
    ): CartResponse =
        cartService.updateProductQty(currentUserId(), clubId, lineId, request.qty)

    @DeleteMapping("/items/{type}/{id}")
    fun deleteItem(
        @RequestParam clubId: Long,
        @PathVariable type: String,
        @PathVariable id: Long
    ): CartResponse =
        cartService.deleteItem(currentUserId(), clubId, type, id)

    @DeleteMapping
    fun clearCart(
        @RequestParam clubId: Long
    ) {
        cartService.clearCart(currentUserId(), clubId)
    }
}
