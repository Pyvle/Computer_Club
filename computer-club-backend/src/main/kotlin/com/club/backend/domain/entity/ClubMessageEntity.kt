package com.club.backend.domain.entity

import com.club.backend.domain.enum.ClubReportStatus
import jakarta.persistence.*
import java.time.Instant

enum class ClubMessageType {
    USER_REPORT,
    PLATFORM_WARNING
}

@Entity
@Table(name = "club_messages")
class ClubMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: ClubEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    var messageType: ClubMessageType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    var author: UserEntity? = null,

    @Column(columnDefinition = "text", nullable = false)
    var message: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    var status: ClubReportStatus? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
