package com.example.computerclub.data.repository

import com.example.computerclub.data.network.CheckoutApi
import com.example.computerclub.data.network.dto.CheckoutRequestDto
import com.example.computerclub.data.network.dto.CheckoutResponseDto
import com.example.computerclub.data.network.dto.PurchaseDetailsDto
import com.example.computerclub.data.network.dto.PurchaseListItemDto

class CheckoutRepository(private val api: CheckoutApi) {
    suspend fun checkout(idempotencyKey: String, clubId: Long): CheckoutResponseDto =
        api.checkout(idempotencyKey, CheckoutRequestDto(clubId))

    suspend fun getMyPurchases(): List<PurchaseListItemDto> = api.getMyPurchases()

    suspend fun getPurchaseDetails(id: Long): PurchaseDetailsDto = api.getPurchaseDetails(id)

    suspend fun payPurchase(id: Long): PurchaseListItemDto = api.payPurchase(id)

    suspend fun cancelPurchase(id: Long): PurchaseListItemDto = api.cancelPurchase(id)
}
