package com.club.backend.domain.entity

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class CartItemSeatId(
    @Column(name = "cart_item_id")
    var cartItemId: Long = 0,

    @Column(name = "seat_id")
    var seatId: Long = 0
) : Serializable

@Entity
@Table(name = "cart_item_seats")
class CartItemSeatEntity(
    @EmbeddedId
    var id: CartItemSeatId = CartItemSeatId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cartItemId")
    @JoinColumn(name = "cart_item_id")
    var item: CartItemEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("seatId")
    @JoinColumn(name = "seat_id")
    var seat: SeatEntity
)
