package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "cart_product_lines")
class CartProductLineEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: CartEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity,

    @Column(nullable = false)
    var qty: Int,

    @Column(name = "price_rub_snapshot", nullable = false)
    var priceRubSnapshot: Int,

    @Column(name = "title_snapshot", nullable = false, length = 160)
    var titleSnapshot: String
)