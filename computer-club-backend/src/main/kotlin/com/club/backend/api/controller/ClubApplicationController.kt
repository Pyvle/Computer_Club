package com.club.backend.api.controller

import com.club.backend.api.dto.*
import com.club.backend.service.ClubApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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

    /** Создать черновик заявки (DRAFT). */
    @PostMapping("/draft")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDraft(@RequestBody req: CreateClubApplicationRequest): ClubApplicationResponse =
        clubApplicationService.createDraft(currentUserId(), req)

    /** Обновить черновик или заявку, требующую доработки. */
    @PutMapping("/{applicationId}")
    fun update(
        @PathVariable applicationId: Long,
        @RequestBody @Valid req: UpdateClubApplicationRequest
    ): ClubApplicationResponse =
        clubApplicationService.updateApplication(currentUserId(), applicationId, req)

    /** Отправить черновик на рассмотрение: DRAFT -> PENDING. */
    @PostMapping("/{applicationId}/submit")
    fun submit(@PathVariable applicationId: Long): ClubApplicationResponse =
        clubApplicationService.submitDraft(currentUserId(), applicationId)

    /** Отправить повторно после доработки: REVISION_REQUESTED -> PENDING. */
    @PostMapping("/{applicationId}/resubmit")
    fun resubmit(
        @PathVariable applicationId: Long,
        @RequestBody @Valid req: UpdateClubApplicationRequest
    ): ClubApplicationResponse =
        clubApplicationService.resubmit(currentUserId(), applicationId, req)

    /** Мои заявки (история). */
    @GetMapping("/my")
    fun my(): List<ClubApplicationResponse> =
        clubApplicationService.listMyApplications(currentUserId())
}
