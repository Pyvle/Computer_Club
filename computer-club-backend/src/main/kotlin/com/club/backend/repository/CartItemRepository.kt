package com.club.backend.repository

import com.club.backend.domain.entity.CartItemEntity
import com.club.backend.domain.entity.CartItemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CartItemRepository : JpaRepository<CartItemEntity, Long> {

    @Query("select i from CartItemEntity i where i.cart.id = :cartId and i.itemType = :type order by i.id asc")
    fun findAllByCartIdAndTypeOrderByIdAsc(
        @Param("cartId") cartId: Long,
        @Param("type") type: CartItemType
    ): List<CartItemEntity>

    @Query(
        """
        select i
        from CartItemEntity i
        where i.cart.id = :cartId
          and i.itemType = com.club.backend.domain.entity.CartItemType.PRODUCT
          and i.product.id = :productId
        order by i.id asc
        """
    )
    fun findAllProductItemsByCartIdAndProductIdOrderByIdAsc(
        @Param("cartId") cartId: Long,
        @Param("productId") productId: Long
    ): List<CartItemEntity>

    @Modifying
    @Query("delete from CartItemEntity i where i.cart.id = :cartId")
    fun deleteAllByCartId(@Param("cartId") cartId: Long): Int
}
