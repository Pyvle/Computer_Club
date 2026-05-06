package com.club.backend.repository

import com.club.backend.api.dto.admin.DashboardBookingPreview
import com.club.backend.domain.entity.BookingEntity
import com.club.backend.domain.enum.BookingStatus
import com.club.backend.repository.projection.BusySeatProjection
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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
     * Возвращает все брони клуба для отчётов с предзагрузкой связей.
     * Фильтрация по from/to/status выполняется в сервисе — PostgreSQL не может
     * определить тип nullable-параметра в конструкции `:param is null`.
     */
    @Query(
        """
        select distinct b
        from BookingEntity b
        left join fetch b.user u
        left join fetch b.seats bs
        left join fetch bs.seat s
        where b.club.id = :clubId
        order by b.startAt desc
        """
    )
    fun findAllForAdmin(@Param("clubId") clubId: Long): List<BookingEntity>

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

    /** Переводит UPCOMING → ACTIVE для броней, у которых startAt уже наступил. */
    @Modifying
    @Query("""
        update BookingEntity b
        set b.status = com.club.backend.domain.enum.BookingStatus.ACTIVE,
            b.updatedAt = :now
        where b.status = com.club.backend.domain.enum.BookingStatus.UPCOMING
          and b.startAt <= :now
    """)
    fun activateStarted(@Param("now") now: LocalDateTime): Int

    /** Переводит ACTIVE → DONE для броней, у которых endAt уже прошёл. */
    @Modifying
    @Query("""
        update BookingEntity b
        set b.status = com.club.backend.domain.enum.BookingStatus.DONE,
            b.updatedAt = :now
        where b.status = com.club.backend.domain.enum.BookingStatus.ACTIVE
          and b.endAt <= :now
    """)
    fun completeFinished(@Param("now") now: LocalDateTime): Int

    /** Отменяет все UPCOMING/ACTIVE брони заказа одним UPDATE-запросом. */
    @Modifying
    @Query("""
        update BookingEntity b
        set b.status = com.club.backend.domain.enum.BookingStatus.CANCELED
        where b.purchase.id = :purchaseId
          and b.status in (
              com.club.backend.domain.enum.BookingStatus.UPCOMING,
              com.club.backend.domain.enum.BookingStatus.ACTIVE
          )
    """)
    fun cancelByPurchaseId(@Param("purchaseId") purchaseId: Long): Int

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

    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.user.id = :userId")
    fun countByUserId(@Param("userId") userId: Long): Long

    @Query("SELECT MAX(b.createdAt) FROM BookingEntity b WHERE b.user.id = :userId")
    fun findLatestCreatedAtByUserId(@Param("userId") userId: Long): LocalDateTime?

    @Query("""
        SELECT b FROM BookingEntity b
        JOIN FETCH b.club
        WHERE b.user.id = :userId
        ORDER BY b.startAt DESC
    """)
    fun findRecentByUserId(@Param("userId") userId: Long, pageable: Pageable): List<BookingEntity>

    @Query("""
        select distinct b from BookingEntity b
        join fetch b.club
        left join fetch b.seats bs
        left join fetch bs.seat s
        where b.user.id = :userId
        order by b.startAt desc
    """)
    fun findAllByUserIdFetch(@Param("userId") userId: Long): List<BookingEntity>

    /** Возвращает брони клуба, активные в указанный момент времени (для снимка занятости зала). */
    @Query(
        """
        select distinct b
        from BookingEntity b
        left join fetch b.user u
        left join fetch b.seats bs
        left join fetch bs.seat s
        left join fetch b.purchase p
        where b.club.id = :clubId
          and b.status in ('UPCOMING', 'ACTIVE')
          and b.startAt <= :at
          and b.endAt > :at
        """
    )
    fun findAtMoment(
        @Param("clubId") clubId: Long,
        @Param("at") at: LocalDateTime
    ): List<BookingEntity>

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

    @Query("""
        select distinct b from BookingEntity b
        left join fetch b.seats bs
        left join fetch bs.seat s
        where b.club.id = :clubId and b.user.id = :userId
        order by b.startAt desc
    """)
    fun findAllByClubIdAndUserIdFetch(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long
    ): List<BookingEntity>
}