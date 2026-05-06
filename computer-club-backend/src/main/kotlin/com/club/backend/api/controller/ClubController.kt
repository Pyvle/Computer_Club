package com.club.backend.api.controller

import com.club.backend.api.dto.AvailableClubResponse
import com.club.backend.api.dto.ClubResponse
import com.club.backend.api.dto.CreateReportRequest
import com.club.backend.api.dto.admin.SeatSpecResponse
import com.club.backend.service.ClubService
import com.club.backend.service.ClubSeatSpecService
import com.club.backend.service.ClubUserReportService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.context.SecurityContextHolder

@RestController
@RequestMapping("/api/v1/clubs")
class ClubController(
    private val clubService: ClubService,
    private val seatSpecService: ClubSeatSpecService,
    private val reportService: ClubUserReportService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized")
        return principal.toLong()
    }

    @GetMapping
    fun getClubs(): List<ClubResponse> = clubService.getAllActive()

    @GetMapping("/{clubId}")
    fun getClub(@PathVariable clubId: Long): ClubResponse = clubService.getById(clubId)

    @GetMapping("/available")
    fun available(): List<AvailableClubResponse> =
        clubService.getAvailableForUser(currentUserId())

    @GetMapping("/{clubId}/seat-specs")
    fun seatSpecs(@PathVariable clubId: Long): List<SeatSpecResponse> =
        seatSpecService.getByClub(clubId)

    @PostMapping("/{clubId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitReport(@PathVariable clubId: Long, @RequestBody req: CreateReportRequest) {
        reportService.submit(clubId, currentUserId(), req)
    }
}