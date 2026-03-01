package com.club.backend.repository

import com.club.backend.domain.entity.CartEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CartRepository : JpaRepository<CartEntity, Long> {
    fun findByUserIdAndClubId(userId: Long, clubId: Long): Optional<CartEntity>
}