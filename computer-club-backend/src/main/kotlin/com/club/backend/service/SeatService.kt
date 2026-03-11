package com.club.backend.service

import com.club.backend.api.dto.SeatAvailabilityResponse
import com.club.backend.api.dto.SeatResponse
import com.club.backend.repository.BookingRepository
import com.club.backend.repository.SeatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val bookingRepository: BookingRepository,
    private val clubAccessService: ClubAccessService
) {

    @Transactional(readOnly = true)
    fun getClubSeats(userId: Long?, clubId: Long): List<SeatResponse> {
        if (userId != null) clubAccessService.ensureNotBlocked(userId, clubId)
        return seatRepository.findAllByClubIdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId).map {
            SeatResponse(
                id = it.id!!,
                label = it.label,
                type = it.type
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAvailability(userId: Long?, clubId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<SeatAvailabilityResponse> {
        if (userId != null) clubAccessService.ensureNotBlocked(userId, clubId)
        require(endAt.isAfter(startAt)) { "endAt must be after startAt" }

        val seats = seatRepository.findAllByClubIdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId)
        val busyIds = bookingRepository.findBusySeatIds(clubId, startAt, endAt)
            .map { it.getSeatId() }
            .toSet()

        return seats.map {
            SeatAvailabilityResponse(
                seatId = it.id!!,
                label = it.label,
                type = it.type,
                isAvailable = it.id !in busyIds
            )
        }
    }
}