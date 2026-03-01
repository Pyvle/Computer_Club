package com.club.backend.service

import com.club.backend.repository.CartBookingLineRepository
import com.club.backend.repository.CartProductLineRepository
import com.club.backend.repository.CartRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CartCleanupService(
    private val cartRepository: CartRepository,
    private val cartBookingLineRepository: CartBookingLineRepository,
    private val cartProductLineRepository: CartProductLineRepository
) {

    @Transactional
    fun clearByUserAndClub(userId: Long, clubId: Long) {
        val cart = cartRepository.findByUserIdAndClubId(userId, clubId)
            .orElseThrow { EntityNotFoundException("Cart not found") }

        val deletedBookings = cartBookingLineRepository.deleteAllByCartId(cart.id!!)
        val deletedProducts = cartProductLineRepository.deleteAllByCartId(cart.id!!)

        cart.updatedAt = LocalDateTime.now()
        cartRepository.saveAndFlush(cart)

        println("Cart cleared: cartId=${cart.id}, bookingsDeleted=$deletedBookings, productsDeleted=$deletedProducts")
    }
}
