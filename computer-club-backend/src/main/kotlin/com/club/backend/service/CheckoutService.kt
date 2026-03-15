package com.club.backend.service

import com.club.backend.api.dto.BookingItemResponse
import com.club.backend.api.dto.CheckoutRequest
import com.club.backend.api.dto.CheckoutResponse
import com.club.backend.api.dto.ProductItemResponse
import com.club.backend.api.dto.PurchaseDetailsResponse
import com.club.backend.api.dto.PurchaseListItemResponse
import com.club.backend.domain.entity.*
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.domain.enum.ProductOrderStatus
import com.club.backend.domain.enum.ReadyByPolicy
import com.club.backend.repository.*
import com.club.backend.repository.ClubSeatPriceRepository
import com.club.backend.repository.ClubTimePackageRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

private const val FALLBACK_RATE = 200 // используется если цены за места не заданы

@Service
class CheckoutService(
    private val cartRepository: CartRepository,
    private val cartBookingLineRepository: CartBookingLineRepository,
    private val cartBookingSeatRepository: CartBookingSeatRepository,
    private val cartProductLineRepository: CartProductLineRepository,
    private val bookingRepository: BookingRepository,
    private val bookingSeatRepository: BookingSeatRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productOrderRepository: ProductOrderRepository,
    private val productOrderItemRepository: ProductOrderItemRepository,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val cartCleanupService: CartCleanupService,
    private val seatRepository: SeatRepository,
    private val clubAccessService: ClubAccessService,
    private val auditService: AuditService,
    private val seatPriceRepository: ClubSeatPriceRepository,
    private val timePackageRepository: ClubTimePackageRepository
) {

    @Transactional
    fun checkout(userId: Long, request: CheckoutRequest): CheckoutResponse {
        clubAccessService.ensureNotBlocked(userId, request.clubId)

        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val club = clubRepository.findById(request.clubId).orElseThrow { EntityNotFoundException("Club not found") }

        val cart = cartRepository.findByUserIdAndClubId(userId, request.clubId)
            .orElseThrow { EntityNotFoundException("Cart not found") }

        val cartBookingLines = cartBookingLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!)
        val cartProductLines = cartProductLineRepository.findAllByCartIdOrderByIdAsc(cart.id!!)

        require(cartBookingLines.isNotEmpty() || cartProductLines.isNotEmpty()) { "Cart is empty" }

        // Пессимистичная блокировка seats — защита от race condition при одновременном checkout
        val allSeatIds = cartBookingLines
            .flatMap { line -> cartBookingSeatRepository.findAllByLine_Id(line.id!!).map { it.seat.id!! } }
            .distinct()
            .sorted()

        if (allSeatIds.isNotEmpty()) {
            seatRepository.findAllByIdForUpdate(allSeatIds)
        }

        // 1) Проверка конфликтов мест
        cartBookingLines.forEach { line ->
            val seatIds = cartBookingSeatRepository.findAllByLine_Id(line.id!!).map { it.seat.id!! }
            if (seatIds.isNotEmpty()) {
                val busyIds = bookingRepository.findBusySeatIds(club.id!!, line.startAt, line.endAt)
                    .map { it.getSeatId() }
                    .toSet()

                val conflict = seatIds.firstOrNull { it in busyIds }
                require(conflict == null) { "Seat $conflict is already booked for selected time" }
            }
        }

        // 2) Подсчёт суммы бронирований
        // тарифная сетка: цены за типы мест и активные пакеты
        val seatPriceMap = seatPriceRepository.findAllByClub_Id(club.id!!)
            .associate { it.seatType to it.pricePerHourRub }
        val standardRate = if (seatPriceMap.isNotEmpty()) seatPriceMap.values.min() else FALLBACK_RATE
        val packageRateMap = timePackageRepository
            .findAllByClub_IdAndIsActiveTrueOrderBySortOrderAscIdAsc(club.id!!)
            .associate { it.hours to it.pricePerHourRub }

        var bookingTotal = 0
        val bookingDrafts = mutableListOf<Triple<CartBookingLineEntity, Int, Int>>() // line, lineTotal, baseRate

        cartBookingLines.forEach { line ->
            val lineSeats = cartBookingSeatRepository.findAllByLine_Id(line.id!!)
            require(lineSeats.isNotEmpty()) { "Select at least one seat for booking line ${line.id}" }

            val hours = Duration.between(line.startAt, line.endAt).toMinutes().toDouble() / 60.0
            // базовая ставка: из пакета или стандартная
            val baseRate = if (line.packageHours != null) packageRateMap[line.packageHours] ?: standardRate else standardRate
            // итог = сумма по каждому месту: (baseRate + надбавка за тип) × часы
            val lineTotal = lineSeats.sumOf { cartSeat ->
                val seatTypePrice = seatPriceMap[cartSeat.seat.type] ?: standardRate
                val surcharge = maxOf(0, seatTypePrice - standardRate)
                ceil(hours * (baseRate + surcharge)).toInt()
            }
            bookingTotal += lineTotal
            bookingDrafts += Triple(line, lineTotal, baseRate)
        }

        // 3) Подсчёт суммы товаров
        val productsTotal = cartProductLines.sumOf { it.qty * it.priceRubSnapshot }
        val total = bookingTotal + productsTotal

        // 4) Создаём purchase (mock-оплата)
        val purchase = purchaseRepository.save(
            PurchaseEntity(
                user = user,
                club = club,
                createdAt = LocalDateTime.now(),
                bookingTotalRub = bookingTotal,
                productsTotalRub = productsTotal,
                totalRub = total,
                paymentMethod = request.paymentMethod,
                paymentStatus = PaymentStatus.CREATED,
                externalPaymentId = null
            )
        )

        // 5) Создаём bookings + booking_seats
        bookingDrafts.forEach { (line, lineTotal, baseRate) ->
            val booking = bookingRepository.save(
                BookingEntity(
                    user = user,
                    club = club,
                    purchase = purchase,
                    startAt = line.startAt,
                    endAt = line.endAt,
                    packageHours = line.packageHours,
                    rateRubPerHourSnapshot = baseRate,
                    totalRubSnapshot = lineTotal,
                    status = BookingStatus.UPCOMING,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )

            val lineSeats = cartBookingSeatRepository.findAllByLine_Id(line.id!!)
            lineSeats.forEach { cartSeat ->
                bookingSeatRepository.save(
                    BookingSeatEntity(
                        id = BookingSeatId(booking.id!!, cartSeat.seat.id!!),
                        booking = booking,
                        seat = cartSeat.seat
                    )
                )
            }
        }

        // 6) Создаём product_order + items
        if (cartProductLines.isNotEmpty()) {
            val productOrder = productOrderRepository.save(
                ProductOrderEntity(
                    purchase = purchase,
                    user = user,
                    club = club,
                    createdAt = LocalDateTime.now(),
                    readyBy = null,
                    readyByPolicy = ReadyByPolicy.ASAP,
                    status = ProductOrderStatus.NOT_READY,
                    totalRubSnapshot = productsTotal
                )
            )

            cartProductLines.forEach { line ->
                productOrderItemRepository.save(
                    ProductOrderItemEntity(
                        productOrder = productOrder,
                        product = line.product,
                        titleSnapshot = line.titleSnapshot,
                        priceRubSnapshot = line.priceRubSnapshot,
                        qty = line.qty
                    )
                )
            }
        }

        // 7) ОЧИСТКА КОРЗИНЫ ЧЕРЕЗ ЕДИНЫЙ СЕРВИС (вместо дублирования)
        cartCleanupService.clearByUserAndClub(userId, request.clubId)

        auditService.log(
            actorUserId = userId,
            clubId = request.clubId,
            action = "CHECKOUT_PAID",
            entityType = "Purchase",
            entityId = purchase.id!!.toString(),
            before = null,
            after = mapOf(
                "purchaseId" to purchase.id!!,
                "bookingTotalRub" to purchase.bookingTotalRub,
                "productsTotalRub" to purchase.productsTotalRub,
                "totalRub" to purchase.totalRub
            )
        )

        return CheckoutResponse(
            purchaseId = purchase.id!!,
            paymentStatus = purchase.paymentStatus.name,
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub
        )
    }

    @Transactional(readOnly = true)
    fun history(userId: Long): List<PurchaseListItemResponse> {
        return purchaseRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map {
            PurchaseListItemResponse(
                purchaseId = it.id!!,
                clubId = it.club.id!!,
                createdAt = it.createdAt.toString(),
                bookingTotalRub = it.bookingTotalRub,
                productsTotalRub = it.productsTotalRub,
                totalRub = it.totalRub,
                paymentStatus = it.paymentStatus.name
            )
        }
    }

    @Transactional(readOnly = true)
    fun purchaseDetails(userId: Long, purchaseId: Long): PurchaseDetailsResponse {
        val purchase = purchaseRepository.findById(purchaseId).orElseThrow()

        if (purchase.user.id != userId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden")

        val bookings = bookingRepository.findByUserIdAndPurchaseId(userId, purchaseId)

        val bookingItems = bookings.map { b ->
            BookingItemResponse(
                bookingId = b.id!!,
                startAt = b.startAt.toString(),
                endAt = b.endAt.toString(),
                seatIds = b.seats.map { it.seat.id!! },
                seatLabels = b.seats.map { it.seat.label },
                totalRub = b.totalRubSnapshot
            )
        }

        val order = productOrderRepository.findByUserIdAndPurchaseId(userId, purchaseId)

        val productItems = if (order != null) {
            productOrderItemRepository.findAllByOrderIdFetchProduct(order.id!!).map { i ->
                val unitRub = i.priceRubSnapshot
                val totalRub = unitRub * i.qty

                ProductItemResponse(
                    productId = i.product.id!!,
                    name = i.titleSnapshot,
                    qty = i.qty,
                    unitRub = unitRub,
                    totalRub = totalRub
                )
            }
        } else emptyList()

        return PurchaseDetailsResponse(
            purchaseId = purchase.id!!,
            clubId = purchase.club.id!!,
            createdAt = purchase.createdAt.toString(),
            paymentStatus = purchase.paymentStatus.name,
            bookingItems = bookingItems,
            productItems = productItems,
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub
        )
    }

    @Transactional
    fun cancel(userId: Long, purchaseId: Long): PurchaseListItemResponse {
        val purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found") }
        if (purchase.user.id != userId)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden")
        if (purchase.paymentStatus == PaymentStatus.CANCELED)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already cancelled")

        purchase.paymentStatus = PaymentStatus.CANCELED
        purchaseRepository.save(purchase)

        // освобождаем места — отменяем все UPCOMING/ACTIVE брони одним запросом
        bookingRepository.cancelByPurchaseId(purchaseId)

        return PurchaseListItemResponse(
            purchaseId = purchase.id!!,
            clubId = purchase.club.id!!,
            createdAt = purchase.createdAt.toString(),
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub,
            paymentStatus = purchase.paymentStatus.name
        )
    }

    @Transactional
    fun pay(userId: Long, purchaseId: Long): PurchaseListItemResponse {
        val purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found") }
        if (purchase.user.id != userId)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden")
        if (purchase.paymentStatus == PaymentStatus.PAID)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already paid")
        if (purchase.paymentStatus == PaymentStatus.CANCELED)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Purchase is cancelled")
        purchase.paymentStatus = PaymentStatus.PAID
        purchaseRepository.save(purchase)
        return PurchaseListItemResponse(
            purchaseId = purchase.id!!,
            clubId = purchase.club.id!!,
            createdAt = purchase.createdAt.toString(),
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub,
            paymentStatus = purchase.paymentStatus.name
        )
    }
}
