package com.club.backend.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class BookingSeatId(
    @Column(name = "booking_id")
    var bookingId: Long = 0,

    @Column(name = "seat_id")
    var seatId: Long = 0
) : Serializable