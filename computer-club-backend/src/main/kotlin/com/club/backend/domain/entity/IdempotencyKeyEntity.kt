package com.club.backend.domain.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKeyEntity(
    @Id
    @Column(name = "id", length = 128)
    var id: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(nullable = false, length = 200)
    var endpoint: String,

    @Column(name = "request_hash", nullable = false, length = 64)
    var requestHash: String,

    @Column(name = "status_code", nullable = false)
    var statusCode: Int,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb", nullable = false)
    var responseBody: JsonNode,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime
)
