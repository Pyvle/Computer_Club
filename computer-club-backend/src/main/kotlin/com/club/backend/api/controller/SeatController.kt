package com.club.backend.api.controller

import com.club.backend.api.dto.SeatAvailabilityRequest
import com.club.backend.api.dto.SeatAvailabilityResponse
import com.club.backend.api.dto.SeatResponse
import com.club.backend.service.SeatService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/seats")
class SeatController(
    private val seatService: SeatService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    fun getSeats(@PathVariable clubId: Long): List<SeatResponse> =
        seatService.getClubSeats(currentUserId(), clubId)

    @PostMapping("/availability")
    fun getAvailability(
        @PathVariable clubId: Long,
        @Valid @RequestBody request: SeatAvailabilityRequest
    ): List<SeatAvailabilityResponse> =
        seatService.getAvailability(currentUserId(), clubId, request.startAt, request.endAt)
}