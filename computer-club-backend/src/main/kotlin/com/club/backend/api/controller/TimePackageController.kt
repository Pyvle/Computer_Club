package com.club.backend.api.controller

import com.club.backend.api.dto.TimePackageResponse
import com.club.backend.service.TimePackageService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/time-packages")
class TimePackageController(
    private val service: TimePackageService
) {

    @GetMapping
    fun list(@PathVariable clubId: Long): List<TimePackageResponse> =
        service.listActive(clubId)
}
