package com.club.backend.api.controller

import com.club.backend.api.dto.*
import com.club.backend.domain.entity.ClubApplicationStatus
import com.club.backend.repository.ClubApplicationRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import com.club.backend.service.AdminAuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1")
class MeController(
    private val userRepository: UserRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val clubApplicationRepository: ClubApplicationRepository,
    private val adminAuthService: AdminAuthService
) {
    private fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal?.toString()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        return principal.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
    }

    @GetMapping("/me")
    fun me(): MeResponse {
        val user = userRepository.findById(currentUserId()).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }
        return MeResponse(
            id = user.id!!,
            phone = user.phone,
            username = user.username
        )
    }

    /** Возвращает контекст пользователя для ролевой маршрутизации на фронтенде. */
    @GetMapping("/me/context")
    fun context(): UserContextResponse {
        val userId = currentUserId()
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }

        val clubs = clubStaffRepository.findAllByIdUserId(userId).map { staff ->
            ClubMembership(
                clubId = staff.club.id!!,
                clubName = staff.club.name,
                role = staff.role.name
            )
        }

        // последняя не-APPROVED заявка пользователя
        val activeApplication = clubApplicationRepository
            .findAllByApplicant_IdOrderByCreatedAtDesc(userId)
            .firstOrNull { it.status != ClubApplicationStatus.APPROVED }
            ?.let { app ->
                ActiveApplicationBrief(
                    applicationId = app.id!!,
                    clubName = app.clubName,
                    status = app.status.name,
                    decisionComment = app.decisionComment
                )
            }

        return UserContextResponse(
            userId = user.id!!,
            phone = user.phone,
            email = user.email,
            globalRole = user.globalRole.name,
            clubs = clubs,
            hasPassword = user.passwordHash != null,
            activeApplication = activeApplication
        )
    }

    /** Устанавливает пароль пользователю, вошедшему через OTP. */
    @PostMapping("/me/set-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setPassword(@RequestBody @Valid req: SetPasswordRequest) {
        adminAuthService.setPassword(currentUserId(), req.password)
    }

    /** Проверяет пароль пользователя (step-up). Не меняет роли и токен. */
    @PostMapping("/me/verify-password")
    fun verifyPassword(@RequestBody @Valid req: VerifyPasswordRequest): Map<String, Boolean> {
        val ok = adminAuthService.verifyPassword(currentUserId(), req.password)
        if (!ok) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password")
        return mapOf("ok" to true)
    }
}
