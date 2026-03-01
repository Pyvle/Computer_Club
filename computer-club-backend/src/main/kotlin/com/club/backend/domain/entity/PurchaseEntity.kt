package com.club.backend.domain.entity

import com.club.backend.domain.enum.PaymentMethod
import com.club.backend.domain.enum.PaymentStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "purchases")
class PurchaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "booking_total_rub", nullable = false)
    var bookingTotalRub: Int = 0,

    @Column(name = "products_total_rub", nullable = false)
    var productsTotalRub: Int = 0,

    @Column(name = "total_rub", nullable = false)
    var totalRub: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    var paymentMethod: PaymentMethod = PaymentMethod.CARD,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 24)
    var paymentStatus: PaymentStatus = PaymentStatus.CREATED,

    @Column(name = "external_payment_id", length = 128)
    var externalPaymentId: String? = null
)
