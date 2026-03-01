package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "cart_booking_seats")
class CartBookingSeatEntity(
    @EmbeddedId
    var id: CartBookingSeatId = CartBookingSeatId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cartBookingLineId")
    @JoinColumn(name = "cart_booking_line_id")
    var line: CartBookingLineEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seatId")
    @JoinColumn(name = "seat_id")
    var seat: SeatEntity
)