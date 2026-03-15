package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "club_time_packages")
class ClubTimePackageEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false)
    var hours: Int,

    /** Цена за один час (₽). Итоговая стоимость = pricePerHourRub * hours. */
    @Column(name = "price_per_hour_rub", nullable = false)
    var pricePerHourRub: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    /** Начало окна доступности (null = без ограничения). */
    @Column(name = "available_from")
    var availableFrom: LocalTime? = null,

    /** Конец окна доступности (null = без ограничения). Может быть меньше availableFrom — тогда окно переходит через полночь. */
    @Column(name = "available_to")
    var availableTo: LocalTime? = null
)
