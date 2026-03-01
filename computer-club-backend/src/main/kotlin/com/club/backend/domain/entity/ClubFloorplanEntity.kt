package com.club.backend.domain.entity

import com.club.backend.domain.enum.FloorplanStatus
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "club_floorplans")
class ClubFloorplanEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "club_id", nullable = false)
    var clubId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FloorplanStatus,

    @Column(nullable = false)
    var width: Int,

    @Column(nullable = false)
    var height: Int,

    @Column(name = "grid_size", nullable = false)
    var gridSize: Int = 10,

    @Column(nullable = false)
    var version: Int = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var data: JsonNode,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)