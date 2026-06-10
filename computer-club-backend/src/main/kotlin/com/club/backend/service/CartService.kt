package com.club.backend.service

import com.club.backend.api.dto.cart.*
import com.club.backend.domain.entity.*
import com.club.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
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
    private val cartItemRepository: CartItemRepository,
    private val cartItemSeatRepository: CartItemSeatRepository,
    private val bookingRepository: BookingRepository,
    private val seatTypeSettingRepository: ClubSeatTypeSettingRepository,
    private val timePackageRepository: ClubTimePackageRepository
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

        cartItemRepository.save(
            CartItemEntity(
                cart = cart,
                itemType = CartItemType.BOOKING,
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
        val line = cartItemRepository.findById(lineId)
            .orElseThrow { EntityNotFoundException("Cart booking line not found") }
        require(line.cart.id == cart.id) { "Line does not belong to this cart" }
        require(line.itemType == CartItemType.BOOKING) { "Line is not a booking item" }

        cartItemSeatRepository.deleteAllByItem_Id(lineId)

        if (req.seatIds.isNotEmpty()) {
            val uniqueSeatIds = req.seatIds.distinct()
            val seats = seatRepository.findAllById(uniqueSeatIds).filter { it.isActive }
            require(seats.size == uniqueSeatIds.size) { "Some seatIds are invalid or inactive" }

            // проверяем принадлежность места клубу
            seats.forEach { seat ->
                require(seat.club.id == clubId) { "Seat ${seat.id} belongs to another club" }
            }

            // 1) проверяем конфликты с реальными бронями
            val busyIds = bookingRepository.findBusySeatIds(clubId, line.startAt!!, line.endAt!!)
                .map { it.getSeatId() }
                .toSet()
            val conflictWithExistingBooking = uniqueSeatIds.firstOrNull { it in busyIds }
            require(conflictWithExistingBooking == null) {
                "Seat $conflictWithExistingBooking is already booked for selected time"
            }

            // 2) предотвращаем конфликты внутри корзины
            val otherLines = cartItemRepository.findAllByCartIdAndTypeOrderByIdAsc(cart.id!!, CartItemType.BOOKING)
                .filter { it.id != lineId }
            for (other in otherLines) {
                val overlaps = other.startAt!!.isBefore(line.endAt!!) && line.startAt!!.isBefore(other.endAt!!)
                if (!overlaps) continue
                val otherSeatIds = cartItemSeatRepository.findAllByItem_Id(other.id!!)
                    .map { it.seat.id!! }
                    .toHashSet()
                val conflictInCart = uniqueSeatIds.firstOrNull { it in otherSeatIds }
                require(conflictInCart == null) {
                    "Seat $conflictInCart conflicts with another booking in your cart"
                }
            }

            // 3) сохраняем выбор мест
            seats.forEach { seat ->
                cartItemSeatRepository.save(
                    CartItemSeatEntity(
                        id = CartItemSeatId(
                            cartItemId = line.id!!,
                            seatId = seat.id!!
                        ),
                        item = line,
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

        val existingLines = cartItemRepository
            .findAllProductItemsByCartIdAndProductIdOrderByIdAsc(cart.id!!, req.productId)

        val existing = existingLines.firstOrNull()
        if (existing != null) {
            existing.qty = existing.qty!! + req.qty
            existing.priceRubSnapshot = cp.priceRub
            existing.titleSnapshot = product.title
            cartItemRepository.save(existing)

            // дубликаты не должны появиться при UNIQUE-индексе — страхуемся слиянием
            if (existingLines.size > 1) {
                cartItemRepository.deleteAll(existingLines.drop(1))
            }
        } else {
            cartItemRepository.save(
                CartItemEntity(
                    cart = cart,
                    itemType = CartItemType.PRODUCT,
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
        val line = cartItemRepository.findById(lineId)
            .orElseThrow { EntityNotFoundException("Cart product line not found") }
        require(line.cart.id == cart.id) { "Line does not belong to this cart" }
        require(line.itemType == CartItemType.PRODUCT) { "Line is not a product item" }

        if (qty <= 0) {
            cartItemRepository.delete(line)
        } else {
            line.qty = qty
            cartItemRepository.save(line)
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
                val line = cartItemRepository.findById(id)
                    .orElseThrow { EntityNotFoundException("Booking line not found") }
                require(line.cart.id == cart.id) { "Line does not belong to this cart" }
                require(line.itemType == CartItemType.BOOKING) { "Line is not a booking item" }
                cartItemRepository.delete(line)
            }
            "product" -> {
                val line = cartItemRepository.findById(id)
                    .orElseThrow { EntityNotFoundException("Product line not found") }
                require(line.cart.id == cart.id) { "Line does not belong to this cart" }
                require(line.itemType == CartItemType.PRODUCT) { "Line is not a product item" }
                cartItemRepository.delete(line)
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

        cartItemRepository.deleteAllByCartId(cart.id!!)
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
        val pricing = bookingPricing(cart.club.id!!)
        val bookingLines = cartItemRepository.findAllByCartIdAndTypeOrderByIdAsc(cart.id!!, CartItemType.BOOKING).map { line ->
            val lineSeats = cartItemSeatRepository.findAllByItem_Id(line.id!!)
            val seatIds = lineSeats.map { it.seat.id!! }
            val hours = Duration.between(line.startAt!!, line.endAt!!).toMinutes().toDouble() / 60.0
            CartBookingLineResponse(
                lineId = line.id!!,
                startAt = line.startAt!!.toString(),
                endAt = line.endAt!!.toString(),
                packageHours = line.packageHours,
                seatIds = seatIds,
                lineTotalRub = lineSeats.sumOf { pricing.seatTotalRub(hours, it.seat.type, line.packageHours) }
            )
        }

        val productLines = cartItemRepository.findAllByCartIdAndTypeOrderByIdAsc(cart.id!!, CartItemType.PRODUCT).map { line ->
            CartProductLineResponse(
                lineId = line.id!!,
                productId = line.product!!.id!!,
                title = line.titleSnapshot!!,
                qty = line.qty!!,
                priceRub = line.priceRubSnapshot!!,
                lineTotalRub = line.qty!! * line.priceRubSnapshot!!
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

    private fun bookingPricing(clubId: Long): BookingPricing {
        val seatPriceMap = seatTypeSettingRepository.findAllByClub_Id(clubId)
            .mapNotNull { setting -> setting.pricePerHourRub?.let { setting.seatType to it } }
            .toMap()
        val packageRateMap = timePackageRepository
            .findAllByClub_IdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId)
            .associate { it.hours to it.pricePerHourRub }
        return BookingPricing(seatPriceMap, packageRateMap)
    }
}
