package com.club.backend.api.controller

import com.club.backend.api.dto.FloorplanResponse
import com.club.backend.api.dto.FloorplanWithAvailabilityResponse
import com.club.backend.service.ClubAccessService
import com.club.backend.service.FloorplanService
import org.springframework.web.bind.annotation.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/clubs/{clubId}")
class FloorplanController(
    private val floorplanService: FloorplanService,
    private val clubAccessService: ClubAccessService,
    private val jwtService: com.club.backend.security.JwtService
) {

    @GetMapping("/floorplan")
    fun getPublished(
        @PathVariable clubId: Long,
        @RequestHeader("Authorization", required = false) authorization: String?
    ): FloorplanResponse {
        val userId = jwtService.extractUserIdOrNull(authorization)
        if (userId != null) {
            clubAccessService.ensureNotBlocked(userId, clubId)
        }
        return floorplanService.getPublished(clubId)
    }
    @GetMapping("/floorplan-with-availability")
    fun getPublishedWithAvailability(
        @PathVariable clubId: Long,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime,
        @RequestHeader("Authorization", required = false) authorization: String?
    ): FloorplanWithAvailabilityResponse {
        val userId = jwtService.extractUserIdOrNull(authorization)
        if (userId != null) {
            clubAccessService.ensureNotBlocked(userId, clubId)
        }
        return floorplanService.getPublishedWithAvailability(clubId, from, to)
    }
}
