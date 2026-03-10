package com.club.backend.repository

import com.club.backend.api.dto.admin.DashboardPurchasePreview
import com.club.backend.domain.entity.PurchaseEntity
import com.club.backend.domain.enum.PaymentStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PurchaseRepository : JpaRepository<PurchaseEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<PurchaseEntity>

    /**
     * Возвращает все покупки клуба для отчётов с предзагрузкой связей.
     * Фильтрация по from/to/status выполняется в сервисе — PostgreSQL не может
     * определить тип nullable-параметра в конструкции `:param is null`.
     */
    @Query(
        """
        select p
        from PurchaseEntity p
        join fetch p.user u
        where p.club.id = :clubId
        order by p.createdAt desc
        """
    )
    fun findAllForAdmin(@Param("clubId") clubId: Long): List<PurchaseEntity>

    /** Загружает покупку конкретного клуба с user и club; нет — возвращает null. */
    @Query("""
        select p
        from PurchaseEntity p
        join fetch p.user
        join fetch p.club
        where p.id = :id
          and p.club.id = :clubId
    """)
    fun findByIdAndClubIdFetch(@Param("id") id: Long, @Param("clubId") clubId: Long): PurchaseEntity?

    // считаем по таймзоне сервера — для MVP достаточно
    @Query("SELECT COALESCE(SUM(p.totalRub), 0) FROM PurchaseEntity p WHERE p.club.id = :clubId AND p.paymentStatus = 'PAID' AND p.createdAt >= :from AND p.createdAt < :to")
    fun sumPaidRevenue(
        @Param("clubId") clubId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): Long

    @Query("SELECT COUNT(p) FROM PurchaseEntity p WHERE p.club.id = :clubId AND p.paymentStatus = 'CREATED'")
    fun countPendingByClubId(@Param("clubId") clubId: Long): Long

    // JPQL-проекция — избегает дублей строк и пагинации в памяти
    @Query("""
        SELECT new com.club.backend.api.dto.admin.DashboardPurchasePreview(
            p.id, u.phone, p.totalRub, p.paymentStatus, p.createdAt
        )
        FROM PurchaseEntity p JOIN p.user u
        WHERE p.club.id = :clubId
        ORDER BY p.id DESC
    """)
    fun findRecentPreviews(
        @Param("clubId") clubId: Long,
        pageable: Pageable
    ): List<DashboardPurchasePreview>
}