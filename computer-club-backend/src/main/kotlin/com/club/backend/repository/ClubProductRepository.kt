package com.club.backend.repository

import com.club.backend.domain.entity.ClubProductEntity
import com.club.backend.domain.entity.ClubProductId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubProductRepository : JpaRepository<ClubProductEntity, ClubProductId> {

    @Query(
        """
        select cp
        from ClubProductEntity cp
        join fetch cp.product p
        join fetch p.category c
        where cp.club.id = :clubId
          and cp.isAvailable = true
          and p.isActive = true
          and c.isActive = true
        order by c.sortOrder asc, c.id asc, p.id asc
        """
    )
    fun findMenuByClubId(@Param("clubId") clubId: Long): List<ClubProductEntity>

    /** Возвращает все позиции клуба для админ-каталога, включая недоступные; загружает product + category. */
    @Query(
        """
        select cp
        from ClubProductEntity cp
        join fetch cp.product p
        join fetch p.category c
        where cp.club.id = :clubId
        order by p.id asc
        """
    )
    fun findAllByClubIdWithProductAndCategory(@Param("clubId") clubId: Long): List<ClubProductEntity>

    fun existsByIdProductId(productId: Long): Boolean

    @Modifying
    @Query("delete from ClubProductEntity cp where cp.id.productId = :productId")
    fun deleteAllByProductId(@Param("productId") productId: Long)

    @Modifying
    @Query("delete from ClubProductEntity cp where cp.id.productId in :productIds")
    fun deleteAllByProductIdIn(@Param("productIds") productIds: List<Long>)
}