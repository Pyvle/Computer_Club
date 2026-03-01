package com.club.backend.repository

import com.club.backend.domain.entity.PurchaseEntity
import com.club.backend.domain.enum.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PurchaseRepository : JpaRepository<PurchaseEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<PurchaseEntity>

    /** Возвращает покупки клуба для отчётов; фильтры by from/to/status опциональны. */
    @Query(
        """
        select p
        from PurchaseEntity p
        where p.club.id = :clubId
          and (:from is null or p.createdAt >= :from)
          and (:to is null or p.createdAt <= :to)
          and (:status is null or p.paymentStatus = :status)
        order by p.createdAt desc
        """
    )
    fun findForAdmin(
        @Param("clubId") clubId: Long,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        @Param("status") status: PaymentStatus?
    ): List<PurchaseEntity>
}