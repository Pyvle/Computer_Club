package com.club.backend.api.controller

import com.club.backend.api.dto.SeatPriceResponse
import com.club.backend.service.SeatPriceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/seat-prices")
class SeatPriceController(
    private val service: SeatPriceService
) {

    @GetMapping
    fun list(@PathVariable clubId: Long): List<SeatPriceResponse> =
        service.list(clubId)
}
