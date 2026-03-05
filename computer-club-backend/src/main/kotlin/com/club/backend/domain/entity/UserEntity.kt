package com.club.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = true, unique = true, length = 32)
    var phone: String? = null,

    @Column(nullable = false, length = 64)
    var username: String,

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false, length = 32)
    var globalRole: GlobalRole = GlobalRole.USER,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 255)
    var email: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class GlobalRole {
    USER,
    GLOBAL_ADMIN
}