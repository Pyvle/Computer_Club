package com.club.backend.repository

import com.club.backend.domain.entity.CartBookingSeatEntity
import com.club.backend.domain.entity.CartBookingSeatId
import org.springframework.data.jpa.repository.JpaRepository

interface CartBookingSeatRepository : JpaRepository<CartBookingSeatEntity, CartBookingSeatId> {
    fun findAllByLine_Id(lineId: Long): List<CartBookingSeatEntity>
    fun deleteAllByLine_Id(lineId: Long)
}