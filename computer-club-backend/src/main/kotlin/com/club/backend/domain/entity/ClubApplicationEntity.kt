package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "club_applications")
class ClubApplicationEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    var applicant: UserEntity,

    @Column(name = "club_name", nullable = false, length = 120)
    var clubName: String,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(name = "location_text", length = 255)
    var locationText: String? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: ClubApplicationStatus = ClubApplicationStatus.PENDING,

    @Column(name = "decision_comment", columnDefinition = "text")
    var decisionComment: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    var decidedBy: UserEntity? = null,

    @Column(name = "decided_at")
    var decidedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_club_id")
    var createdClub: ClubEntity? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ClubApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
