package com.club.backend.service

import com.club.backend.domain.entity.UserFavoriteClubEntity
import com.club.backend.domain.entity.UserFavoriteClubId
import com.club.backend.repository.UserFavoriteClubRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FavoriteClubService(
    private val repo: UserFavoriteClubRepository
) {

    @Transactional(readOnly = true)
    fun getFavoriteClubIds(userId: Long): List<Long> =
        repo.findAllByIdUserId(userId).map { it.id.clubId }

    @Transactional
    fun addFavorite(userId: Long, clubId: Long) {
        val id = UserFavoriteClubId(userId = userId, clubId = clubId)
        if (!repo.existsById(id)) {
            repo.save(UserFavoriteClubEntity(id = id))
        }
    }

    @Transactional
    fun removeFavorite(userId: Long, clubId: Long) {
        repo.deleteById(UserFavoriteClubId(userId = userId, clubId = clubId))
    }
}
