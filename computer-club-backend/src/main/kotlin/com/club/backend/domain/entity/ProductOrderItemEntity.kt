package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_order_items")
class ProductOrderItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_order_id", nullable = false)
    var productOrder: ProductOrderEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    var product: ProductEntity?,

    @Column(name = "title_snapshot", nullable = false, length = 160)
    var titleSnapshot: String,

    @Column(name = "price_rub_snapshot", nullable = false)
    var priceRubSnapshot: Int,

    @Column(nullable = false)
    var qty: Int
)
