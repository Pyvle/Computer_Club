package com.club.backend.domain.entity

import com.club.backend.domain.enum.ProductOrderStatus
import com.club.backend.domain.enum.ReadyByPolicy
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "product_orders")
class ProductOrderEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false, unique = true)
    var purchase: PurchaseEntity,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ready_by")
    var readyBy: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ready_by_policy", nullable = false, length = 24)
    var readyByPolicy: ReadyByPolicy = ReadyByPolicy.ASAP,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var status: ProductOrderStatus = ProductOrderStatus.NOT_READY,

    @Column(name = "total_rub_snapshot", nullable = false)
    var totalRubSnapshot: Int = 0
)
