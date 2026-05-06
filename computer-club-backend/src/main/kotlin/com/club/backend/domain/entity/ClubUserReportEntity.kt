package com.club.backend.domain.entity

import com.club.backend.domain.enum.ClubReportStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "club_user_reports")
class ClubUserReportEntity(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    val club: ClubEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(columnDefinition = "text", nullable = false)
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ClubReportStatus = ClubReportStatus.NEW,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
