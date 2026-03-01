package com.example.computerclub.data.network.dto

import kotlinx.serialization.Serializable

@Serializable data class OtpRequestDto(val phone: String)
@Serializable data class OtpRequestResponse(val challengeId: Long, val resendInSeconds: Long, val debugCode: String? = null)

@Serializable data class OtpVerifyDto(val challengeId: Long, val code: String)
@Serializable data class RefreshDto(val refreshToken: String)
@Serializable data class LogoutDto(val refreshToken: String)

@Serializable data class AuthTokensResponse(val accessToken: String, val refreshToken: String)

@Serializable data class MeResponse(val id: Long, val phone: String, val username: String)