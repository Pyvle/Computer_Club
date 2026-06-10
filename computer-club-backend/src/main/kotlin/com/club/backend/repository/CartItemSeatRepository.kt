package com.club.backend.repository

import com.club.backend.domain.entity.CartItemSeatEntity
import com.club.backend.domain.entity.CartItemSeatId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CartItemSeatRepository : JpaRepository<CartItemSeatEntity, CartItemSeatId> {
    fun findAllByItem_Id(itemId: Long): List<CartItemSeatEntity>
    fun deleteAllByItem_Id(itemId: Long)

    @Modifying
    @Query("DELETE FROM CartItemSeatEntity cis WHERE cis.seat.id = :seatId")
    fun deleteAllBySeatId(seatId: Long)
}
