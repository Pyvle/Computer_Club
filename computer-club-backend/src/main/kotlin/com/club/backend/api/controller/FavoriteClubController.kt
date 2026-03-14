package com.club.backend.api.controller

import com.club.backend.service.FavoriteClubService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/me/favorites")
class FavoriteClubController(
    private val favoriteClubService: FavoriteClubService
) {

    private fun currentUserId(): Long =
        SecurityContextHolder.getContext().authentication?.principal?.toString()?.toLong()
            ?: throw IllegalStateException("Unauthorized")

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getFavorites(): List<Long> =
        favoriteClubService.getFavoriteClubIds(currentUserId())

    @PutMapping("/{clubId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addFavorite(@PathVariable clubId: Long) =
        favoriteClubService.addFavorite(currentUserId(), clubId)

    @DeleteMapping("/{clubId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFavorite(@PathVariable clubId: Long) =
        favoriteClubService.removeFavorite(currentUserId(), clubId)
}
