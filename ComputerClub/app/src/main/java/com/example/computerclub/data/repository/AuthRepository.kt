package com.example.computerclub.data.repository

import com.example.computerclub.data.local.TokenStore
import com.example.computerclub.data.network.AuthApi
import com.example.computerclub.data.network.dto.*

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: TokenStore
) {
    suspend fun requestOtp(phone: String): OtpRequestResponse =
        api.requestOtp(OtpRequestDto(phone))

    suspend fun verifyOtp(challengeId: Long, code: String) {
        val pair = api.verifyOtp(OtpVerifyDto(challengeId, code))
        tokenStore.save(pair.accessToken, pair.refreshToken)
    }

    suspend fun logout() {
        val refresh = tokenStore.getRefresh()
        if (!refresh.isNullOrBlank()) api.logout(LogoutDto(refresh))
        tokenStore.clear()
    }
}