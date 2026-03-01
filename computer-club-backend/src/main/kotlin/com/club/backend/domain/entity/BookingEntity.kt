package com.club.backend.domain.entity

import com.club.backend.domain.enum.BookingStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "bookings")
class BookingEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime,

    @Column(name = "end_at", nullable = false)
    var endAt: LocalDateTime,

    @Column(name = "package_hours")
    var packageHours: Int? = null,

    @Column(name = "rate_rub_per_hour_snapshot", nullable = false)
    var rateRubPerHourSnapshot: Int = 0,

    @Column(name = "total_rub_snapshot", nullable = false)
    var totalRubSnapshot: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: BookingStatus = BookingStatus.UPCOMING,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var seats: MutableSet<BookingSeatEntity> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    var purchase: PurchaseEntity? = null
)