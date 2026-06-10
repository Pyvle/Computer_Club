package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

enum class CartItemType {
    BOOKING,
    PRODUCT
}

@Entity
@Table(name = "cart_items")
class CartItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: CartEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 16)
    var itemType: CartItemType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: ProductEntity? = null,

    @Column
    var qty: Int? = null,

    @Column(name = "price_rub_snapshot")
    var priceRubSnapshot: Int? = null,

    @Column(name = "title_snapshot", length = 160)
    var titleSnapshot: String? = null,

    @Column(name = "start_at")
    var startAt: LocalDateTime? = null,

    @Column(name = "end_at")
    var endAt: LocalDateTime? = null,

    @Column(name = "package_hours")
    var packageHours: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
