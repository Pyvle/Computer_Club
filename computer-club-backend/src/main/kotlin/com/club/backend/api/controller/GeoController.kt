package com.club.backend.api.controller

import com.club.backend.api.dto.AddressSearchItem
import com.club.backend.service.GeoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/geo")
class GeoController(
    private val geoService: GeoService
) {

    @GetMapping("/search")
    fun search(@RequestParam query: String): List<AddressSearchItem> =
        geoService.search(query)
}
