package com.club.backend.service

import com.club.backend.api.dto.admin.AdminSeatResponse
import com.club.backend.api.dto.admin.CreateSeatRequest
import com.club.backend.api.dto.admin.UpdateSeatRequest
import com.club.backend.domain.entity.SeatEntity
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.SeatRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeatAdminService(
    private val clubRepository: ClubRepository,
    private val seatRepository: SeatRepository,
    private val auditService: AuditService
) {

    fun list(clubId: Long): List<AdminSeatResponse> =
        seatRepository.findAllByClubIdOrderBySortOrderAscIdAsc(clubId).map { it.toDto() }

    @Transactional
    fun create(actorUserId: Long, clubId: Long, req: CreateSeatRequest): AdminSeatResponse {
        if (seatRepository.existsByClubIdAndLabel(clubId, req.label.trim())) {
            throw IllegalArgumentException("Seat label already exists in this club")
        }
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club $clubId not found") }
        val seat = SeatEntity(
            club = club,
            label = req.label.trim(),
            type = req.type,
            isActive = true,
            sortOrder = req.sortOrder
        )
        val saved = seatRepository.save(seat)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "SEAT_CREATE",
            entityType = "Seat",
            entityId = saved.id!!.toString(),
            before = null,
            after = saved.toDto()
        )
        return saved.toDto()
    }

    @Transactional
    fun update(actorUserId: Long, clubId: Long, seatId: Long, req: UpdateSeatRequest): AdminSeatResponse {
        val seat = seatRepository.findById(seatId).orElseThrow { EntityNotFoundException("Seat $seatId not found") }
        if (seat.club.id != clubId) throw EntityNotFoundException("Seat $seatId not found for club $clubId")

        val before = seat.toDto()
        seat.label = req.label.trim()
        seat.type = req.type
        seat.sortOrder = req.sortOrder
        seat.isActive = req.isActive
        val saved = seatRepository.save(seat)

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "SEAT_UPDATE",
            entityType = "Seat",
            entityId = saved.id!!.toString(),
            before = before,
            after = saved.toDto()
        )

        return saved.toDto()
    }

    @Transactional
    fun archive(actorUserId: Long, clubId: Long, seatId: Long) {
        val seat = seatRepository.findById(seatId).orElseThrow { EntityNotFoundException("Seat $seatId not found") }
        if (seat.club.id != clubId) throw EntityNotFoundException("Seat $seatId not found for club $clubId")

        val before = seat.toDto()
        seat.isActive = false
        val saved = seatRepository.save(seat)

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "SEAT_ARCHIVE",
            entityType = "Seat",
            entityId = saved.id!!.toString(),
            before = before,
            after = saved.toDto()
        )
    }

    private fun SeatEntity.toDto() = AdminSeatResponse(
        id = requireNotNull(id),
        label = label,
        type = type,
        isActive = isActive,
        sortOrder = sortOrder
    )
}
