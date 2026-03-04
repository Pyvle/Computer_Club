package com.club.backend.repository

import com.club.backend.domain.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductRepository : JpaRepository<ProductEntity, Long> {

    /** Возвращает все товары для админ-экранов, включая неактивные; загружает category. */
    @Query(
        """
        select p
        from ProductEntity p
        join fetch p.category c
        order by p.id asc
        """
    )
    fun findAllWithCategoryOrderByIdAsc(): List<ProductEntity>

    fun existsByCategoryId(categoryId: Long): Boolean
}