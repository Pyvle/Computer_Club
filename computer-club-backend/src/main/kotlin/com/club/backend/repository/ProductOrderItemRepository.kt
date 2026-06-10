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
        where i.purchase.id = :purchaseId
        """
    )
    fun findAllByPurchaseIdFetchProduct(@Param("purchaseId") purchaseId: Long): List<ProductOrderItemEntity>

    @Query(
        """
        select i
        from ProductOrderItemEntity i
        join fetch i.purchase p
        where p.id in :purchaseIds
        """
    )
    fun findByPurchaseIds(@Param("purchaseIds") purchaseIds: Collection<Long>): List<ProductOrderItemEntity>
}
