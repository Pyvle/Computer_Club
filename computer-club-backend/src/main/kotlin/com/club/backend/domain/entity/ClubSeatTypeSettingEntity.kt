package com.club.backend.domain.entity

import com.club.backend.domain.enum.SeatType
import jakarta.persistence.*

@Entity
@Table(
    name = "club_seat_type_settings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["club_id", "seat_type"])]
)
class ClubSeatTypeSettingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    var seatType: SeatType,

    @Column(name = "price_per_hour_rub")
    var pricePerHourRub: Int? = null,

    @Column(nullable = false, length = 100)
    var title: String = "",

    @Column(name = "specs_json", columnDefinition = "text", nullable = false)
    var specsJson: String = "[]"
)
