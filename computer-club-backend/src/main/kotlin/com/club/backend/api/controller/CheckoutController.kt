package com.club.backend.api.controller

import com.club.backend.api.dto.CheckoutRequest
import com.club.backend.api.dto.CheckoutResponse
import com.club.backend.api.dto.PurchaseDetailsResponse
import com.club.backend.api.dto.PurchaseListItemResponse
import com.club.backend.api.dto.UserBookingHistoryItemResponse
import com.club.backend.service.CheckoutService
import jakarta.validation.Valid
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class CheckoutController(
    private val checkoutService: CheckoutService
) {
    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized")
        return principal.toLong()
    }

    @PostMapping("/checkout")
    fun checkout(
        @Valid @RequestBody request: CheckoutRequest,
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: String?
    ): CheckoutResponse {
        val userId = currentUserId()
        return try {
            checkoutService.checkout(userId, request, idempotencyKey)
        } catch (e: DataIntegrityViolationException) {
            // гонка двух одинаковых запросов: транзакция откатилась, читаем победившую запись
            checkoutService.findIdempotentResponse(userId, idempotencyKey, request) ?: throw e
        }
    }

    @GetMapping("/purchases")
    fun purchases(): List<PurchaseListItemResponse> =
        checkoutService.history(currentUserId())

    @GetMapping("/purchases/{id}")
    fun purchaseDetails(@PathVariable("id") id: Long): PurchaseDetailsResponse =
        checkoutService.purchaseDetails(currentUserId(), id)

    @GetMapping("/me/bookings")
    fun myBookings(): List<UserBookingHistoryItemResponse> =
        checkoutService.userBookings(currentUserId())

    @PostMapping("/purchases/{id}/pay")
    fun pay(@PathVariable("id") id: Long): PurchaseListItemResponse =
        checkoutService.pay(currentUserId(), id)

    @PostMapping("/purchases/{id}/cancel")
    fun cancel(@PathVariable("id") id: Long): PurchaseListItemResponse =
        checkoutService.cancel(currentUserId(), id)
}
