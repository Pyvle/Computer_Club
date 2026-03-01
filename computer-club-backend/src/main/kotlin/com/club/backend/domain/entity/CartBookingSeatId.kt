package com.club.backend.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class CartBookingSeatId(
    @Column(name = "cart_booking_line_id")
    var cartBookingLineId: Long = 0,

    @Column(name = "seat_id")
    var seatId: Long = 0
) : Serializable