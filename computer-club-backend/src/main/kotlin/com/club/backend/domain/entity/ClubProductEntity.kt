package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "club_products")
class ClubProductEntity(
    @EmbeddedId
    var id: ClubProductId = ClubProductId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("clubId")
    @JoinColumn(name = "club_id")
    var club: ClubEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    var product: ProductEntity,

    @Column(name = "price_rub", nullable = false)
    var priceRub: Int,

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = true
)