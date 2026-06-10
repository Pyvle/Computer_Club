package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "product_order_items")
class ProductOrderItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    var purchase: PurchaseEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    var product: ProductEntity?,

    @Column(name = "product_order_id_snapshot")
    var productOrderIdSnapshot: Long? = null,

    @Column(name = "product_order_created_at")
    var productOrderCreatedAt: LocalDateTime? = null,

    @Column(name = "title_snapshot", nullable = false, length = 160)
    var titleSnapshot: String,

    @Column(name = "price_rub_snapshot", nullable = false)
    var priceRubSnapshot: Int,

    @Column(nullable = false)
    var qty: Int
)
