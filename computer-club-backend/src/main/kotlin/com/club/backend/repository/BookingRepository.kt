package com.club.backend.repository

import com.club.backend.api.dto.admin.DashboardBookingPreview
import com.club.backend.domain.entity.BookingEntity
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.repository.projection.BusySeatProjection
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface BookingRepository : JpaRepository<BookingEntity, Long> {

    @Query(
        """
        select bs.seat.id as seatId
        from BookingEntity b
        join b.seats bs
        where b.club.id = :clubId
          and b.status in ('UPCOMING', 'ACTIVE')
          and b.startAt < :endAt
          and b.endAt > :startAt
        """
    )
    fun findBusySeatIds(
        @Param("clubId") clubId: Long,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime
    ): List<BusySeatProjection>

    /**
     * Возвращает брони клуба для отчётов; фильтры by from/to/status опциональны,
     * from/to фильтруют по startAt брони.
     */
    @Query(
        """
        select distinct b
        from BookingEntity b
        left join fetch b.user u
        left join fetch b.seats bs
        left join fetch bs.seat s
        where b.club.id = :clubId
          and (:from is null or b.startAt >= :from)
          and (:to is null or b.startAt <= :to)
          and (:status is null or b.status = :status)
        order by b.startAt desc
        """
    )
    fun findForAdmin(
        @Param("clubId") clubId: Long,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        @Param("status") status: BookingStatus?
    ): List<BookingEntity>

    @Query(
        """
        select distinct b
        from BookingEntity b
        left join fetch b.user u
        left join fetch b.seats bs
        left join fetch bs.seat s
        where b.id = :id and b.club.id = :clubId
        """
    )
    fun findByIdAndClubIdFetch(
        @Param("id") id: Long,
        @Param("clubId") clubId: Long
    ): BookingEntity?

    @Query(
        """
    select distinct b
    from BookingEntity b
    left join fetch b.seats bs
    left join fetch bs.seat s
    where b.user.id = :userId
      and b.purchase.id = :purchaseId
    """
    )
    fun findByUserIdAndPurchaseId(
        @Param("userId") userId: Long,
        @Param("purchaseId") purchaseId: Long
    ): List<BookingEntity>

    @Query(
        """
        select distinct b
        from BookingEntity b
        left join fetch b.seats bs
        left join fetch bs.seat s
        where b.purchase.id in :purchaseIds
        """
    )
    fun findByPurchaseIds(@Param("purchaseIds") purchaseIds: Collection<Long>): List<BookingEntity>

    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.club.id = :clubId AND b.status = :status")
    fun countByClubIdAndStatus(
        @Param("clubId") clubId: Long,
        @Param("status") status: BookingStatus
    ): Long

    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.club.id = :clubId AND b.status = 'UPCOMING' AND b.startAt >= :from AND b.startAt < :to")
    fun countUpcomingToday(
        @Param("clubId") clubId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): Long

    @Query("SELECT COUNT(bs.seat.id) FROM BookingEntity b JOIN b.seats bs WHERE b.club.id = :clubId AND b.status = 'ACTIVE'")
    fun countOccupiedSeats(@Param("clubId") clubId: Long): Long

    // JPQL-проекция — избегает дублей строк и пагинации в памяти
    @Query("""
        SELECT new com.club.backend.api.dto.admin.DashboardBookingPreview(
            b.id, u.phone, b.startAt, b.endAt, b.status
        )
        FROM BookingEntity b JOIN b.user u
        WHERE b.club.id = :clubId
        ORDER BY b.id DESC
    """)
    fun findRecentPreviews(
        @Param("clubId") clubId: Long,
        pageable: Pageable
    ): List<DashboardBookingPreview>
}