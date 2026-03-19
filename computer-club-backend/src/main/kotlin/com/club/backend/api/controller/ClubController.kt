package com.club.backend.api.controller

import com.club.backend.api.dto.AvailableClubResponse
import com.club.backend.api.dto.ClubResponse
import com.club.backend.api.dto.admin.SeatSpecResponse
import com.club.backend.service.ClubService
import com.club.backend.service.ClubSeatSpecService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/v1/clubs")
class ClubController(
    private val clubService: ClubService,
    private val seatSpecService: ClubSeatSpecService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    fun getClubs(): List<ClubResponse> = clubService.getAllActive()

    @GetMapping("/available")
    fun available(): List<AvailableClubResponse> =
        clubService.getAvailableForUser(currentUserId())

    @GetMapping("/{clubId}/seat-specs")
    fun seatSpecs(@PathVariable clubId: Long): List<SeatSpecResponse> =
        seatSpecService.getByClub(clubId)
}