package com.club.backend.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class ClubProductId(
    @Column(name = "club_id")
    var clubId: Long = 0,

    @Column(name = "product_id")
    var productId: Long = 0
) : Serializable