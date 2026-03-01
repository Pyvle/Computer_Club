package com.club.backend.repository

import com.club.backend.domain.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByPhone(phone: String): Optional<UserEntity>
    fun findByUsername(username: String): Optional<UserEntity>
}

