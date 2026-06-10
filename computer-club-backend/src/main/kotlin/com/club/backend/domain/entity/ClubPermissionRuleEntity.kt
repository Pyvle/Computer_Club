package com.club.backend.domain.entity

import com.club.backend.domain.enum.ClubPermission
import jakarta.persistence.*
import java.time.OffsetDateTime

enum class ClubPermissionRuleType {
    ROLE_DEFAULT,
    USER_OVERRIDE
}

@Entity
@Table(name = "club_permission_rules")
class ClubPermissionRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 16)
    var ruleType: ClubPermissionRuleType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    var club: ClubEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: UserEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    var role: ClubRole? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    var permission: ClubPermission,

    @Column(nullable = false)
    var granted: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
