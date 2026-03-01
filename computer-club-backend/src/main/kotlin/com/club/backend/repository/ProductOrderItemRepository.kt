package com.club.backend.repository

import com.club.backend.domain.entity.ProductOrderItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductOrderItemRepository : JpaRepository<ProductOrderItemEntity, Long> {

    @Query(
        """
        select i
        from ProductOrderItemEntity i
        join fetch i.product p
        where i.productOrder.id = :orderId
        """
    )
    fun findAllByOrderIdFetchProduct(@Param("orderId") orderId: Long): List<ProductOrderItemEntity>
}