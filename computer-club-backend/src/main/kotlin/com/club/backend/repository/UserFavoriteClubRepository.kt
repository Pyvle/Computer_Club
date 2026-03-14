package com.club.backend.repository

import com.club.backend.domain.entity.UserFavoriteClubEntity
import com.club.backend.domain.entity.UserFavoriteClubId
import org.springframework.data.jpa.repository.JpaRepository

interface UserFavoriteClubRepository : JpaRepository<UserFavoriteClubEntity, UserFavoriteClubId> {
    fun findAllByIdUserId(userId: Long): List<UserFavoriteClubEntity>
}
