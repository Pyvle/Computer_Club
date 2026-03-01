package com.club.backend.domain.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
data class ClubStaffId(
    @Column(name = "club_id")
    var clubId: Long = 0,

    @Column(name = "user_id")
    var userId: Long = 0
) : Serializable

enum class ClubRole {
    OWNER,
    ADMIN
}

@Entity
@Table(name = "club_staff")
class ClubStaffEntity(
    @EmbeddedId
    var id: ClubStaffId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("clubId")
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var role: ClubRole,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
