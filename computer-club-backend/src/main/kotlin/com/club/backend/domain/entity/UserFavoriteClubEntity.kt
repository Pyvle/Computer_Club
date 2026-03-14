package com.club.backend.domain.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_favorite_clubs")
class UserFavoriteClubEntity(

    @EmbeddedId
    val id: UserFavoriteClubId,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
