package com.club.backend.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class ClubUserBlockId(
    @Column(name = "club_id")
    val clubId: Long = 0,

    @Column(name = "user_id")
    val userId: Long = 0
) : Serializable
