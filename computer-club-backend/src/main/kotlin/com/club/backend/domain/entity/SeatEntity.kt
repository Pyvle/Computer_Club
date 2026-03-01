package com.club.backend.domain.entity

import com.club.backend.domain.enum.SeatType
import jakarta.persistence.*

@Entity
@Table(name = "seats")
class SeatEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Column(nullable = false, length = 32)
    var label: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var type: SeatType,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
)