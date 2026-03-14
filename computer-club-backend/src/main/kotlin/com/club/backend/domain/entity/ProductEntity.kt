package com.club.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "products")
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: ProductCategoryEntity,

    @Column(nullable = false, length = 160)
    var title: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "image_url", columnDefinition = "text")
    var imageUrl: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)