package com.club.backend.api.controller

import com.club.backend.api.dto.ClubMembership
import com.club.backend.api.dto.MeResponse
import com.club.backend.api.dto.PendingApplicationBrief
import com.club.backend.api.dto.UserContextResponse
import com.club.backend.domain.entity.ClubApplicationStatus
import com.club.backend.repository.ClubApplicationRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1")
class MeController(
    private val userRepository: UserRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val clubApplicationRepository: ClubApplicationRepository
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

        val pendingStatuses = setOf(ClubApplicationStatus.PENDING)
        val pendingApplications = clubApplicationRepository.findAllByApplicant_Id(userId)
            .filter { it.status in pendingStatuses }
            .map { app ->
                PendingApplicationBrief(
                    applicationId = app.id!!,
                    clubName = app.clubName,
                    status = app.status.name
                )
            }

        return UserContextResponse(
            userId = user.id!!,
            phone = user.phone,
            email = user.email,
            globalRole = user.globalRole.name,
            clubs = clubs,
            pendingApplications = pendingApplications
        )
    }
}