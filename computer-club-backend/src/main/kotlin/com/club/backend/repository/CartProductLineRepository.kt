package com.club.backend.repository

import com.club.backend.domain.entity.CartProductLineEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface CartProductLineRepository : JpaRepository<CartProductLineEntity, Long> {

    @Query("select l from CartProductLineEntity l where l.cart.id = :cartId order by l.id asc")
    fun findAllByCartIdOrderByIdAsc(@Param("cartId") cartId: Long): List<CartProductLineEntity>

    @Query("select l from CartProductLineEntity l where l.cart.id = :cartId and l.product.id = :productId")
    fun findByCartIdAndProductId(
        @Param("cartId") cartId: Long,
        @Param("productId") productId: Long
    ): Optional<CartProductLineEntity>

    @Query("select l from CartProductLineEntity l where l.cart.id = :cartId and l.product.id = :productId order by l.id asc")
    fun findAllByCartIdAndProductIdOrderByIdAsc(
        @Param("cartId") cartId: Long,
        @Param("productId") productId: Long
    ): List<CartProductLineEntity>


    @Modifying
    @Query("delete from CartProductLineEntity l where l.cart.id = :cartId")
    fun deleteAllByCartId(@Param("cartId") cartId: Long): Int
}
