package com.club.backend.api.controller

import com.club.backend.api.dto.MeResponse
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
    private val userRepository: UserRepository
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
}