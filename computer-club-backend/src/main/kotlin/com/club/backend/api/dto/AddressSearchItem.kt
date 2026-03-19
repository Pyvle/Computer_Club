package com.club.backend.api.dto

data class AddressSearchItem(
    val addressFull: String,
    val addressShort: String,
    val latitude: Double,
    val longitude: Double
)
