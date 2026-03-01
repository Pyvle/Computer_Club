package com.club.backend.repository

import com.club.backend.domain.entity.ProductCategoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProductCategoryRepository : JpaRepository<ProductCategoryEntity, Long> {
    /** Возвращает все категории для админ-экранов, включая неактивные. */
    fun findAllByOrderBySortOrderAscIdAsc(): List<ProductCategoryEntity>

    /** Возвращает только активные категории для клиентского меню. */
    fun findAllByIsActiveTrueOrderBySortOrderAscIdAsc(): List<ProductCategoryEntity>
}