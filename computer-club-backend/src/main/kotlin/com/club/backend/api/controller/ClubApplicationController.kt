package com.club.backend.api.controller

import com.club.backend.api.dto.ClubApplicationResponse
import com.club.backend.api.dto.CreateClubApplicationRequest
import com.club.backend.service.ClubApplicationService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/club-applications")
class ClubApplicationController(
    private val clubApplicationService: ClubApplicationService
) {

    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw IllegalArgumentException("Unauthorized")
        return principal.toLong()
    }

    /** Подать заявку на создание клуба (для любого авторизованного пользователя). */
    @PostMapping
    fun create(@RequestBody req: CreateClubApplicationRequest): ClubApplicationResponse =
        clubApplicationService.createApplication(currentUserId(), req)

    /** Мои заявки (история). */
    @GetMapping("/my")
    fun my(): List<ClubApplicationResponse> =
        clubApplicationService.listMyApplications(currentUserId())
}
