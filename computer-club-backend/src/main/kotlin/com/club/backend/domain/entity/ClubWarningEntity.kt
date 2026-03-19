package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "club_warning")
class ClubWarningEntity(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    val club: ClubEntity,

    @Column(columnDefinition = "text", nullable = false)
    val message: String,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
