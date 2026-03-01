package com.club.backend.service

import com.club.backend.api.dto.*
import com.club.backend.api.error.ConflictException
import com.club.backend.domain.enum.FloorplanStatus
import com.club.backend.domain.entity.ClubFloorplanEntity
import com.club.backend.repository.ClubFloorplanRepository
import com.club.backend.repository.SeatRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class FloorplanAdminService(
    private val clubFloorplanRepository: ClubFloorplanRepository,
    private val seatRepository: SeatRepository,
    private val objectMapper: ObjectMapper,
    private val auditService: AuditService
) {

    fun list(clubId: Long): List<FloorplanSummaryResponse> =
        clubFloorplanRepository.findAllByClubIdOrderByUpdatedAtDesc(clubId).map { it.toSummary() }

    fun get(clubId: Long, floorplanId: Long): FloorplanResponse =
        clubFloorplanRepository.findById(floorplanId).orElseThrow {
            EntityNotFoundException("Floorplan $floorplanId not found")
        }.also {
            if (it.clubId != clubId) throw EntityNotFoundException("Floorplan $floorplanId not found for club $clubId")
        }.toResponse()

    @Transactional
    fun create(actorUserId: Long, clubId: Long, req: CreateFloorplanRequest): FloorplanResponse {
        val entity = ClubFloorplanEntity(
            clubId = clubId,
            name = req.name.trim(),
            width = req.width,
            height = req.height,
            gridSize = req.gridSize,
            status = FloorplanStatus.DRAFT,
            version = 1,
            data = objectMapper.createObjectNode(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val saved = clubFloorplanRepository.save(entity)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "FLOORPLAN_CREATE",
            entityType = "Floorplan",
            entityId = saved.id!!.toString(),
            before = null,
            after = saved.toResponse()
        )
        return saved.toResponse()
    }

    @Transactional
    fun update(actorUserId: Long, clubId: Long, floorplanId: Long, req: UpdateFloorplanRequest): FloorplanResponse {
        val entity = clubFloorplanRepository.findById(floorplanId).orElseThrow {
            EntityNotFoundException("Floorplan $floorplanId not found")
        }
        if (entity.clubId != clubId) throw EntityNotFoundException("Floorplan $floorplanId not found for club $clubId")
        if (entity.status == FloorplanStatus.ARCHIVED) throw IllegalStateException("Floorplan is archived")
        if (entity.version != req.version) throw ConflictException("Version conflict: expected ${entity.version}, got ${req.version}")

        validateSeatRefs(clubId, req.data)

        val before = entity.toResponse()

        entity.name = req.name.trim()
        entity.width = req.width
        entity.height = req.height
        entity.gridSize = req.gridSize
        entity.data = req.data
        entity.version = entity.version + 1
        entity.updatedAt = OffsetDateTime.now()

        val saved = clubFloorplanRepository.save(entity)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "FLOORPLAN_UPDATE",
            entityType = "Floorplan",
            entityId = saved.id!!.toString(),
            before = before,
            after = saved.toResponse()
        )

        return saved.toResponse()
    }

    @Transactional
    fun clone(actorUserId: Long, clubId: Long, floorplanId: Long, req: CloneFloorplanRequest): FloorplanResponse {
        val src = clubFloorplanRepository.findById(floorplanId).orElseThrow {
            EntityNotFoundException("Floorplan $floorplanId not found")
        }
        if (src.clubId != clubId) throw EntityNotFoundException("Floorplan $floorplanId not found for club $clubId")
        val copy = ClubFloorplanEntity(
            clubId = clubId,
            name = req.name.trim(),
            width = src.width,
            height = src.height,
            gridSize = src.gridSize,
            status = FloorplanStatus.DRAFT,
            version = 1,
            data = src.data,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val saved = clubFloorplanRepository.save(copy)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "FLOORPLAN_CLONE",
            entityType = "Floorplan",
            entityId = saved.id!!.toString(),
            before = src.toResponse(),
            after = saved.toResponse()
        )
        return saved.toResponse()
    }

    @Transactional
    fun publish(actorUserId: Long, clubId: Long, floorplanId: Long): PublishFloorplanResponse {
        val entity = clubFloorplanRepository.findById(floorplanId).orElseThrow {
            EntityNotFoundException("Floorplan $floorplanId not found")
        }
        if (entity.clubId != clubId) throw EntityNotFoundException("Floorplan $floorplanId not found for club $clubId")
        if (entity.status == FloorplanStatus.ARCHIVED) throw IllegalStateException("Floorplan is archived")

        // проверяем корректность перед публикацией
        validateSeatRefs(clubId, entity.data)

        val before = entity.toResponse()

        // снимаем с публикации предыдущую схему
        clubFloorplanRepository.findByClubIdAndStatus(clubId, FloorplanStatus.PUBLISHED)?.let { prev ->
            if (prev.id != entity.id) {
                val prevBefore = prev.toResponse()
                prev.status = FloorplanStatus.DRAFT
                prev.updatedAt = OffsetDateTime.now()
                val prevSaved = clubFloorplanRepository.save(prev)

                auditService.log(
                    actorUserId = actorUserId,
                    clubId = clubId,
                    action = "FLOORPLAN_UNPUBLISH_AUTO",
                    entityType = "Floorplan",
                    entityId = prevSaved.id!!.toString(),
                    before = prevBefore,
                    after = prevSaved.toResponse()
                )
            }
        }

        entity.status = FloorplanStatus.PUBLISHED
        entity.updatedAt = OffsetDateTime.now()
        val saved = clubFloorplanRepository.save(entity)

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "FLOORPLAN_PUBLISH",
            entityType = "Floorplan",
            entityId = saved.id!!.toString(),
            before = before,
            after = saved.toResponse()
        )
        return PublishFloorplanResponse(publishedId = entity.id)
    }

    @Transactional
    fun unpublish(actorUserId: Long, clubId: Long, floorplanId: Long) {
        val entity = clubFloorplanRepository.findById(floorplanId).orElseThrow {
            EntityNotFoundException("Floorplan $floorplanId not found")
        }
        if (entity.clubId != clubId) throw EntityNotFoundException("Floorplan $floorplanId not found for club $clubId")
        if (entity.status == FloorplanStatus.PUBLISHED) {
            val before = entity.toResponse()
            entity.status = FloorplanStatus.DRAFT
            entity.updatedAt = OffsetDateTime.now()
            val saved = clubFloorplanRepository.save(entity)
            auditService.log(
                actorUserId = actorUserId,
                clubId = clubId,
                action = "FLOORPLAN_UNPUBLISH",
                entityType = "Floorplan",
                entityId = saved.id!!.toString(),
                before = before,
                after = saved.toResponse()
            )
        }
    }

    @Transactional
    fun archive(actorUserId: Long, clubId: Long, floorplanId: Long) {
        val entity = clubFloorplanRepository.findById(floorplanId).orElseThrow {
            EntityNotFoundException("Floorplan $floorplanId not found")
        }
        if (entity.clubId != clubId) throw EntityNotFoundException("Floorplan $floorplanId not found for club $clubId")
        val before = entity.toResponse()
        entity.status = FloorplanStatus.ARCHIVED
        entity.updatedAt = OffsetDateTime.now()
        val saved = clubFloorplanRepository.save(entity)

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "FLOORPLAN_ARCHIVE",
            entityType = "Floorplan",
            entityId = saved.id!!.toString(),
            before = before,
            after = saved.toResponse()
        )
    }

    private fun validateSeatRefs(clubId: Long, root: JsonNode) {
        val items = root.get("items") ?: return
        if (!items.isArray) return

        val seatIds = mutableSetOf<Long>()
        for (item in items) {
            val type = item.get("type")?.asText() ?: continue
            if (type != "SEAT") continue

            val seatIdNode = item.get("seatId")
            val seatId = seatIdNode?.asLong()
                ?: throw IllegalArgumentException("SEAT item must have seatId")

            if (!seatIds.add(seatId)) {
                throw IllegalArgumentException("Duplicate seatId in floorplan: $seatId")
            }

            if (!seatRepository.existsByIdAndClubId(seatId, clubId)) {
                throw IllegalArgumentException("Seat $seatId does not belong to club $clubId")
            }
        }
    }

    private fun ClubFloorplanEntity.toSummary() = FloorplanSummaryResponse(
        id = id,
        clubId = clubId,
        name = name,
        status = status,
        version = version,
        updatedAt = updatedAt.toString()
    )

    private fun ClubFloorplanEntity.toResponse() = FloorplanResponse(
        id = id,
        clubId = clubId,
        name = name,
        width = width,
        height = height,
        gridSize = gridSize,
        status = status,
        version = version,
        data = data,
        updatedAt = updatedAt.toString()
    )
}
