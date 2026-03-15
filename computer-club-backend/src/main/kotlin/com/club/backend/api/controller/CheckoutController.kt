package com.club.backend.api.controller

import com.club.backend.api.dto.CheckoutRequest
import com.club.backend.api.dto.CheckoutResponse
import com.club.backend.api.dto.PurchaseDetailsResponse
import com.club.backend.api.dto.PurchaseListItemResponse
import com.club.backend.service.CheckoutService
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class CheckoutController(
    private val checkoutService: CheckoutService
) {
    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @PostMapping("/checkout")
    fun checkout(@Valid @RequestBody request: CheckoutRequest): CheckoutResponse =
        checkoutService.checkout(currentUserId(), request)

    @GetMapping("/purchases")
    fun purchases(): List<PurchaseListItemResponse> =
        checkoutService.history(currentUserId())

    @GetMapping("/purchases/{id}")
    fun purchaseDetails(@PathVariable("id") id: Long): PurchaseDetailsResponse =
        checkoutService.purchaseDetails(currentUserId(), id)

    @PostMapping("/purchases/{id}/pay")
    fun pay(@PathVariable("id") id: Long): PurchaseListItemResponse =
        checkoutService.pay(currentUserId(), id)

    @PostMapping("/purchases/{id}/cancel")
    fun cancel(@PathVariable("id") id: Long): PurchaseListItemResponse =
        checkoutService.cancel(currentUserId(), id)
}
