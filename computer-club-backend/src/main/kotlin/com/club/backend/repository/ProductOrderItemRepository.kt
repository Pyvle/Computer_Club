package com.club.backend.repository

import com.club.backend.domain.entity.ProductOrderItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductOrderItemRepository : JpaRepository<ProductOrderItemEntity, Long> {

    fun existsByProductId(productId: Long): Boolean

    @Query(
        """
        select i
        from ProductOrderItemEntity i
        left join fetch i.product p
        where i.productOrder.id = :orderId
        """
    )
    fun findAllByOrderIdFetchProduct(@Param("orderId") orderId: Long): List<ProductOrderItemEntity>

    @Query(
        """
        select i
        from ProductOrderItemEntity i
        join fetch i.productOrder po
        join fetch po.purchase
        where po.purchase.id in :purchaseIds
        """
    )
    fun findByPurchaseIds(@Param("purchaseIds") purchaseIds: Collection<Long>): List<ProductOrderItemEntity>
}