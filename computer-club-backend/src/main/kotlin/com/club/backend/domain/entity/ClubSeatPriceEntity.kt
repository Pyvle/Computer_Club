package com.club.backend.domain.entity

import com.club.backend.domain.enum.SeatType
import jakarta.persistence.*

@Entity
@Table(name = "club_seat_prices")
class ClubSeatPriceEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var seatType: SeatType,

    @Column(name = "price_per_hour_rub", nullable = false)
    var pricePerHourRub: Int
)
