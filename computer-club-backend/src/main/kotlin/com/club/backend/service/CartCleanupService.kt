package com.club.backend.service

import com.club.backend.repository.CartItemRepository
import com.club.backend.repository.CartRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CartCleanupService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository
) {

    @Transactional
    fun clearByUserAndClub(userId: Long, clubId: Long) {
        val cart = cartRepository.findByUserIdAndClubId(userId, clubId)
            .orElseThrow { EntityNotFoundException("Cart not found") }

        val deletedItems = cartItemRepository.deleteAllByCartId(cart.id!!)

        cart.updatedAt = LocalDateTime.now()
        cartRepository.saveAndFlush(cart)

        logger.info("Cart cleared: cartId=${cart.id}, itemsDeleted=$deletedItems")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CartCleanupService::class.java)
    }
}
