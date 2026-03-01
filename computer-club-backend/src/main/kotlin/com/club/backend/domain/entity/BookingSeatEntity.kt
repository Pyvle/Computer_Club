package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "booking_seats")
class BookingSeatEntity(

    @EmbeddedId
    var id: BookingSeatId = BookingSeatId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    var booking: BookingEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seatId")
    @JoinColumn(name = "seat_id")
    var seat: SeatEntity
)