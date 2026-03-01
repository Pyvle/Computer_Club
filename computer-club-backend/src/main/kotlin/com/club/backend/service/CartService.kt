package com.club.backend.service

import com.club.backend.api.dto.cart.*
import com.club.backend.domain.entity.*
import com.club.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CartService(
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val seatRepository: SeatRepository,
    private val productRepository: ProductRepository,
    private val clubProductRepository: ClubProductRepository,
    private val clubAccessService: ClubAccessService,
    private val cartRepository: CartRepository,
    private val cartBookingLineRepository: CartBookingLineRepository,
    private val cartBookingSeatRepository: CartBookingSeatRepository,
    private val cartProductLineRepository: CartProductLineRepository,
    private val bookingRepository: BookingRepository
) {

    @Transactional
    fun selectClub(userId: Long, clubId: Long): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }

        val cart = cartRepository.findByUserIdAndClubId(userId, clubId).orElseGet {
            cartRepository.save(CartEntity(user = user, club = club))
        }
        cart.updatedAt = LocalDateTime.now()
        return toResponse(cartRepository.save(cart))
    }

    @Transactional(readOnly = true)
    fun getCart(userId: Long, clubId: Long): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val cart = cartRepository.findByUserIdAndClubId(userId, clubId)
            .orElseThrow { EntityNotFoundException("Cart not found. Select club first.") }
        return toResponse(cart)
    }

    @Transactional
    fun addBooking(userId: Long, clubId: Long, req: AddCartBookingRequest): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        require(req.endAt.isAfter(req.startAt)) { "endAt must be after startAt" }
        val cart = getOrCreateCart(userId, clubId)

        cartBookingLineRepository.save(
            CartBookingLineEntity(
                cart = cart,
                startAt = req.startAt,
                endAt = req.endAt,
                packageHours = req.packageHours
            )
        )
        cart.updatedAt = LocalDateTime.now()
        cartRepository.save(cart)
        return toResponse(cart)
    }

    @Transactional
    fun setBookingSeats(userId: Long, clubId: Long, lineId: Long, req: SetCartBookingSeatsRequest): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val cart = getOrCreateCart(userId, clubId)
        val line = cartBookingLineRepository.findById(lineId)
            .orElseThrow { EntityNotFoundException("Cart booking line not found") }
        require(line.cart.id == cart.id) { "Line does not belong to this cart" }

        cartBookingSeatRepository.deleteAllByLine_Id(lineId)

        if (req.seatIds.isNotEmpty()) {
            val uniqueSeatIds = req.seatIds.distinct()
            val seats = seatRepository.findAllById(uniqueSeatIds).filter { it.isActive }
            require(seats.size == uniqueSeatIds.size) { "Some seatIds are invalid or inactive" }

            // проверяем принадлежность места клубу
            seats.forEach { seat ->
                require(seat.club.id == clubId) { "Seat ${seat.id} belongs to another club" }
            }

            // 1) проверяем конфликты с реальными бронями
            val busyIds = bookingRepository.findBusySeatIds(clubId, line.startAt, line.endAt)
                .map { it.getSeatId() }
                .toSet()
            val conflictWithExistingBooking = uniqueSeatIds.firstOrNull { it in busyIds }
            require(conflictWithExistingBooking == null) {
                "Seat $conflictWithExistingBooking is already booked for selected time"
            }

            // 2) предотвращаем конфликты внутри корзины
            val otherLines = cartBookingLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!)
                .filter { it.id != lineId }
            for (other in otherLines) {
                val overlaps = other.startAt.isBefore(line.endAt) && line.startAt.isBefore(other.endAt)
                if (!overlaps) continue
                val otherSeatIds = cartBookingSeatRepository.findAllByLine_Id(other.id!!)
                    .map { it.seat.id!! }
                    .toHashSet()
                val conflictInCart = uniqueSeatIds.firstOrNull { it in otherSeatIds }
                require(conflictInCart == null) {
                    "Seat $conflictInCart conflicts with another booking in your cart"
                }
            }

            // 3) сохраняем выбор мест
            seats.forEach { seat ->
                cartBookingSeatRepository.save(
                    CartBookingSeatEntity(
                        id = CartBookingSeatId(
                            cartBookingLineId = line.id!!,
                            seatId = seat.id!!
                        ),
                        line = line,
                        seat = seat
                    )
                )
            }
        }

        cart.updatedAt = LocalDateTime.now()
        cartRepository.save(cart)
        return toResponse(cart)
    }

    @Transactional
    fun addProduct(userId: Long, clubId: Long, req: AddCartProductRequest): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val cart = getOrCreateCart(userId, clubId)
        val product = productRepository.findById(req.productId)
            .orElseThrow { EntityNotFoundException("Product not found") }

        val cp = clubProductRepository.findById(ClubProductId(clubId, req.productId))
            .orElseThrow { EntityNotFoundException("Product is not available in this club") }

        require(cp.isAvailable) { "Product is not available now" }

        val existingLines = cartProductLineRepository
            .findAllByCartIdAndProductIdOrderByIdAsc(cart.id!!, req.productId)

        val existing = existingLines.firstOrNull()
        if (existing != null) {
            existing.qty += req.qty
            existing.priceRubSnapshot = cp.priceRub
            existing.titleSnapshot = product.title
            cartProductLineRepository.save(existing)

            // дубликаты не должны появиться при UNIQUE-индексе — страхуемся слиянием
            if (existingLines.size > 1) {
                cartProductLineRepository.deleteAll(existingLines.drop(1))
            }
        } else {
            cartProductLineRepository.save(
                CartProductLineEntity(
                    cart = cart,
                    product = product,
                    qty = req.qty,
                    priceRubSnapshot = cp.priceRub,
                    titleSnapshot = product.title
                )
            )
        }

        cart.updatedAt = LocalDateTime.now()
        cartRepository.save(cart)
        return toResponse(cart)
    }

    @Transactional
    fun updateProductQty(userId: Long, clubId: Long, lineId: Long, qty: Int): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val cart = getOrCreateCart(userId, clubId)
        val line = cartProductLineRepository.findById(lineId)
            .orElseThrow { EntityNotFoundException("Cart product line not found") }
        require(line.cart.id == cart.id) { "Line does not belong to this cart" }

        if (qty <= 0) {
            cartProductLineRepository.delete(line)
        } else {
            line.qty = qty
            cartProductLineRepository.save(line)
        }
        cart.updatedAt = LocalDateTime.now()
        cartRepository.save(cart)
        return toResponse(cart)
    }

    @Transactional
    fun deleteItem(userId: Long, clubId: Long, type: String, id: Long): CartResponse {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val cart = getOrCreateCart(userId, clubId)

        when (type.lowercase()) {
            "booking" -> {
                val line = cartBookingLineRepository.findById(id)
                    .orElseThrow { EntityNotFoundException("Booking line not found") }
                require(line.cart.id == cart.id) { "Line does not belong to this cart" }
                cartBookingLineRepository.delete(line)
            }
            "product" -> {
                val line = cartProductLineRepository.findById(id)
                    .orElseThrow { EntityNotFoundException("Product line not found") }
                require(line.cart.id == cart.id) { "Line does not belong to this cart" }
                cartProductLineRepository.delete(line)
            }
            else -> error("type must be booking or product")
        }

        cart.updatedAt = LocalDateTime.now()
        cartRepository.save(cart)
        return toResponse(cart)
    }

    @Transactional
    fun clearCart(userId: Long, clubId: Long) {
        clubAccessService.ensureNotBlocked(userId, clubId)

        val cart = cartRepository.findByUserIdAndClubId(userId, clubId)
            .orElseThrow { EntityNotFoundException("Cart not found") }

        cartBookingLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!).forEach {
            cartBookingLineRepository.delete(it)
        }
        cartProductLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!).forEach {
            cartProductLineRepository.delete(it)
        }
        cart.updatedAt = LocalDateTime.now()
        cartRepository.save(cart)
    }

    private fun getOrCreateCart(userId: Long, clubId: Long): CartEntity {
        return cartRepository.findByUserIdAndClubId(userId, clubId).orElseGet {
            val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
            val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
            cartRepository.save(CartEntity(user = user, club = club))
        }
    }

    @Transactional(readOnly = true)
    fun toResponse(cart: CartEntity): CartResponse {
        val bookingLines = cartBookingLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!).map { line ->
            val seatIds = cartBookingSeatRepository.findAllByLine_Id(line.id!!).map { it.seat.id!! }
            CartBookingLineResponse(
                lineId = line.id!!,
                startAt = line.startAt.toString(),
                endAt = line.endAt.toString(),
                packageHours = line.packageHours,
                seatIds = seatIds
            )
        }

        val productLines = cartProductLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!).map { line ->
            CartProductLineResponse(
                lineId = line.id!!,
                productId = line.product.id!!,
                title = line.titleSnapshot,
                qty = line.qty,
                priceRub = line.priceRubSnapshot,
                lineTotalRub = line.qty * line.priceRubSnapshot
            )
        }

        return CartResponse(
            cartId = cart.id!!,
            userId = cart.user.id!!,
            clubId = cart.club.id!!,
            updatedAt = cart.updatedAt.toString(),
            bookings = bookingLines,
            products = productLines
        )
    }
}