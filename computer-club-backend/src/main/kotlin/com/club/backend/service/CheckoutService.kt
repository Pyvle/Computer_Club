package com.club.backend.service

import com.club.backend.api.dto.BookingItemResponse
import com.club.backend.api.dto.CheckoutRequest
import com.club.backend.api.dto.CheckoutResponse
import com.club.backend.api.dto.ProductItemResponse
import com.club.backend.api.dto.PurchaseDetailsResponse
import com.club.backend.api.dto.PurchaseListItemResponse
import com.club.backend.api.dto.UserBookingHistoryItemResponse
import com.club.backend.domain.entity.*
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.domain.enum.PaymentStatus
import com.club.backend.repository.*
import com.club.backend.repository.ClubTimePackageRepository
import com.club.backend.repository.ClubSeatTypeSettingRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime

private const val CHECKOUT_ENDPOINT = "POST /api/v1/checkout"

@Service
class CheckoutService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val cartItemSeatRepository: CartItemSeatRepository,
    private val bookingRepository: BookingRepository,
    private val bookingSeatRepository: BookingSeatRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productOrderItemRepository: ProductOrderItemRepository,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val cartCleanupService: CartCleanupService,
    private val seatRepository: SeatRepository,
    private val clubAccessService: ClubAccessService,
    private val auditService: AuditService,
    private val seatTypeSettingRepository: ClubSeatTypeSettingRepository,
    private val timePackageRepository: ClubTimePackageRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val objectMapper: ObjectMapper
) {

    private fun hashRequest(request: CheckoutRequest): String {
        val json = objectMapper.writeValueAsString(request)
        val bytes = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Возвращает кэшированный ответ по idempotencyKey или null если запись не найдена. */
    @Transactional(readOnly = true)
    fun findIdempotentResponse(userId: Long, idempotencyKey: String?, request: CheckoutRequest): CheckoutResponse? {
        if (idempotencyKey.isNullOrBlank()) return null
        val existing = idempotencyKeyRepository.findByIdAndUser_IdAndEndpoint(idempotencyKey, userId, CHECKOUT_ENDPOINT)
            ?: return null
        if (existing.requestHash != hashRequest(request)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key was already used with another request")
        }
        return objectMapper.readValue(existing.responseBody, CheckoutResponse::class.java)
    }

    @Transactional
    fun checkout(userId: Long, request: CheckoutRequest, idempotencyKey: String? = null): CheckoutResponse {
        // проверка идемпотентности: если ключ уже использован — вернуть кэшированный ответ
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = idempotencyKeyRepository.findByIdAndUser_IdAndEndpoint(idempotencyKey, userId, CHECKOUT_ENDPOINT)
            if (existing != null) {
                if (existing.requestHash != hashRequest(request)) {
                    throw ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key was already used with another request")
                }
                return objectMapper.readValue(existing.responseBody, CheckoutResponse::class.java)
            }
        }

        clubAccessService.ensureNotBlocked(userId, request.clubId)

        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val club = clubRepository.findById(request.clubId).orElseThrow { EntityNotFoundException("Club not found") }

        val cart = cartRepository.findByUserIdAndClubId(userId, request.clubId)
            .orElseThrow { EntityNotFoundException("Cart not found") }

        val cartBookingLines = cartItemRepository.findAllByCartIdAndTypeOrderByIdAsc(cart.id!!, CartItemType.BOOKING)
        val cartProductLines = cartItemRepository.findAllByCartIdAndTypeOrderByIdAsc(cart.id!!, CartItemType.PRODUCT)

        require(cartBookingLines.isNotEmpty() || cartProductLines.isNotEmpty()) { "Cart is empty" }

        // Пессимистичная блокировка seats — защита от race condition при одновременном checkout
        val allSeatIds = cartBookingLines
            .flatMap { line -> cartItemSeatRepository.findAllByItem_Id(line.id!!).map { it.seat.id!! } }
            .distinct()
            .sorted()

        if (allSeatIds.isNotEmpty()) {
            seatRepository.findAllByIdForUpdate(allSeatIds)
        }

        // 1) Проверка конфликтов мест
        cartBookingLines.forEach { line ->
            val seatIds = cartItemSeatRepository.findAllByItem_Id(line.id!!).map { it.seat.id!! }
            if (seatIds.isNotEmpty()) {
                val busyIds = bookingRepository.findBusySeatIds(club.id!!, line.startAt!!, line.endAt!!)
                    .map { it.getSeatId() }
                    .toSet()

                val conflict = seatIds.firstOrNull { it in busyIds }
                require(conflict == null) { "Seat $conflict is already booked for selected time" }
            }
        }

        // 2) Подсчёт суммы бронирований
        // тарифная сетка: цены за типы мест и активные пакеты
        val seatPriceMap = seatTypeSettingRepository.findAllByClub_Id(club.id!!)
            .mapNotNull { setting -> setting.pricePerHourRub?.let { setting.seatType to it } }
            .toMap()
        val packageRateMap = timePackageRepository
            .findAllByClub_IdAndIsActiveTrueOrderBySortOrderAscIdAsc(club.id!!)
            .associate { it.hours to it.pricePerHourRub }
        val pricing = BookingPricing(seatPriceMap, packageRateMap)

        var bookingTotal = 0
        val bookingDrafts = mutableListOf<Triple<CartItemEntity, Int, Int>>() // line, lineTotal, baseRate

        cartBookingLines.forEach { line ->
            val lineSeats = cartItemSeatRepository.findAllByItem_Id(line.id!!)
            require(lineSeats.isNotEmpty()) { "Select at least one seat for booking line ${line.id}" }

            val hours = Duration.between(line.startAt!!, line.endAt!!).toMinutes().toDouble() / 60.0
            // базовая ставка: из пакета или стандартная
            val baseRate = pricing.baseRate(line.packageHours)
            // итог = сумма по каждому месту: (baseRate + надбавка за тип) × часы
            val lineTotal = lineSeats.sumOf { cartSeat ->
                pricing.seatTotalRub(hours, cartSeat.seat.type, line.packageHours)
            }
            bookingTotal += lineTotal
            bookingDrafts += Triple(line, lineTotal, baseRate)
        }

        // 3) Подсчёт суммы товаров
        val productsTotal = cartProductLines.sumOf { it.qty!! * it.priceRubSnapshot!! }
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
                paymentStatus = PaymentStatus.CREATED
            )
        )

        // 5) Создаём bookings + booking_seats
        bookingDrafts.forEach { (line, lineTotal, baseRate) ->
            val booking = bookingRepository.save(
                BookingEntity(
                    user = user,
                    club = club,
                    purchase = purchase,
                    startAt = line.startAt!!,
                    endAt = line.endAt!!,
                    packageHours = line.packageHours,
                    rateRubPerHourSnapshot = baseRate,
                    totalRubSnapshot = lineTotal,
                    status = BookingStatus.UPCOMING,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )

            val lineSeats = cartItemSeatRepository.findAllByItem_Id(line.id!!)
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

        // 6) Создаём товарные позиции покупки
        if (cartProductLines.isNotEmpty()) {
            cartProductLines.forEach { line ->
                productOrderItemRepository.save(
                    ProductOrderItemEntity(
                        purchase = purchase,
                        product = line.product,
                        titleSnapshot = line.titleSnapshot!!,
                        priceRubSnapshot = line.priceRubSnapshot!!,
                        qty = line.qty!!
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

        val response = CheckoutResponse(
            purchaseId = purchase.id!!,
            paymentStatus = purchase.paymentStatus.name,
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub
        )

        // сохраняем ключ идемпотентности в той же транзакции — при откате покупки ключ тоже откатится
        if (!idempotencyKey.isNullOrBlank()) {
            val now = OffsetDateTime.now()
            idempotencyKeyRepository.save(
                IdempotencyKeyEntity(
                    id = idempotencyKey,
                    user = user,
                    endpoint = CHECKOUT_ENDPOINT,
                    requestHash = hashRequest(request),
                    statusCode = 200,
                    responseBody = objectMapper.writeValueAsString(response),
                    createdAt = now,
                    expiresAt = now.plusHours(24)
                )
            )
        }

        return response
    }

    @Transactional(readOnly = true)
    fun history(userId: Long): List<PurchaseListItemResponse> {
        return purchaseRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map {
            PurchaseListItemResponse(
                purchaseId = it.id!!,
                clubId = it.club.id!!,
                clubName = it.club.name,
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
                totalRub = b.totalRubSnapshot,
                rateRubPerHourSnapshot = b.rateRubPerHourSnapshot,
                packageHours = b.packageHours
            )
        }

        val productItems = productOrderItemRepository.findAllByPurchaseIdFetchProduct(purchaseId).map { i ->
            val unitRub = i.priceRubSnapshot
            val totalRub = unitRub * i.qty

            ProductItemResponse(
                productId = i.product?.id,
                name = i.titleSnapshot,
                qty = i.qty,
                unitRub = unitRub,
                totalRub = totalRub
            )
        }

        return PurchaseDetailsResponse(
            purchaseId = purchase.id!!,
            clubId = purchase.club.id!!,
            clubName = purchase.club.name,
            createdAt = purchase.createdAt.toString(),
            paymentStatus = purchase.paymentStatus.name,
            bookingItems = bookingItems,
            productItems = productItems,
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub
        )
    }

    @Transactional(readOnly = true)
    fun userBookings(userId: Long): List<UserBookingHistoryItemResponse> {
        return bookingRepository.findAllByUserIdFetch(userId).map { booking ->
            UserBookingHistoryItemResponse(
                bookingId = booking.id!!,
                purchaseId = booking.purchase?.id,
                clubId = booking.club.id!!,
                clubName = booking.club.name,
                createdAt = (booking.purchase?.createdAt ?: booking.createdAt).toString(),
                startAt = booking.startAt.toString(),
                endAt = booking.endAt.toString(),
                status = booking.status.name,
                totalRub = booking.totalRubSnapshot,
                rateRubPerHourSnapshot = booking.rateRubPerHourSnapshot,
                packageHours = booking.packageHours,
                seatIds = booking.seats.map { it.seat.id!! },
                seatLabels = booking.seats.map { it.seat.label },
                paymentStatus = booking.purchase?.paymentStatus?.name
            )
        }
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
            clubName = purchase.club.name,
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
            clubName = purchase.club.name,
            createdAt = purchase.createdAt.toString(),
            bookingTotalRub = purchase.bookingTotalRub,
            productsTotalRub = purchase.productsTotalRub,
            totalRub = purchase.totalRub,
            paymentStatus = purchase.paymentStatus.name
        )
    }
}
