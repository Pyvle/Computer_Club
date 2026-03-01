package com.club.backend.repository

import com.club.backend.domain.entity.CartBookingLineEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CartBookingLineRepository : JpaRepository<CartBookingLineEntity, Long> {

    @Query("select l from CartBookingLineEntity l where l.cart.id = :cartId order by l.id asc")
    fun findAllByCartIdOrderByIdAsc(@Param("cartId") cartId: Long): List<CartBookingLineEntity>

    @Modifying
    @Query("delete from CartBookingLineEntity l where l.cart.id = :cartId")
    fun deleteAllByCartId(@Param("cartId") cartId: Long): Int
}
