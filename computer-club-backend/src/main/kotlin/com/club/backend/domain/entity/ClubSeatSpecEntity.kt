package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "club_seat_spec",
    uniqueConstraints = [UniqueConstraint(columnNames = ["club_id", "seat_type"])]
)
class ClubSeatSpecEntity(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    val club: ClubEntity,

    @Column(name = "seat_type", nullable = false, length = 20)
    val seatType: String,

    @Column(nullable = false, length = 100)
    var title: String,

    // JSON-массив [{name, value}, ...]
    @Column(name = "specs_json", columnDefinition = "text", nullable = false)
    var specsJson: String = "[]"
)
