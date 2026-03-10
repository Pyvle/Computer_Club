package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "club_user_blocks")
class ClubUserBlockEntity(
    @EmbeddedId
    var id: ClubUserBlockId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("clubId")
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(name = "is_blocked", nullable = false)
    var isBlocked: Boolean = true,

    @Column(length = 255)
    var reason: String? = null,

    @Column(name = "blocked_until")
    var blockedUntil: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_user_id")
    var blockedBy: UserEntity? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
