package com.club.backend.repository

import com.club.backend.domain.entity.ProductOrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductOrderRepository : JpaRepository<ProductOrderEntity, Long> {

    @Query("select po from ProductOrderEntity po where po.purchase.id = :purchaseId and po.user.id = :userId")
    fun findByUserIdAndPurchaseId(
        @Param("userId") userId: Long,
        @Param("purchaseId") purchaseId: Long
    ): ProductOrderEntity?
}