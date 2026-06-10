package com.club.backend.service

import com.club.backend.api.dto.SeatMaxAvailabilityResponse
import com.club.backend.api.dto.SeatAvailabilityResponse
import com.club.backend.api.dto.SeatResponse
import com.club.backend.repository.BookingRepository
import com.club.backend.repository.SeatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
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

    @Transactional(readOnly = true)
    fun getMaxAvailability(userId: Long?, clubId: Long, startAt: LocalDateTime): List<SeatMaxAvailabilityResponse> {
        if (userId != null) clubAccessService.ensureNotBlocked(userId, clubId)

        val seats = seatRepository.findAllByClubIdAndIsActiveTrueOrderBySortOrderAscIdAsc(clubId)
        val windowsBySeatId = bookingRepository.findSeatBookingWindowsAfter(clubId, startAt)
            .groupBy { it.getSeatId() }

        return seats.map { seat ->
            val seatId = seat.id!!
            val windows = windowsBySeatId[seatId].orEmpty()
            val hasConflictAtStart = windows.any { window ->
                !window.getStartAt().isAfter(startAt) && window.getEndAt().isAfter(startAt)
            }

            if (hasConflictAtStart) {
                return@map SeatMaxAvailabilityResponse(
                    seatId = seatId,
                    label = seat.label,
                    type = seat.type,
                    isAvailableAtStart = false,
                    maxAvailableMinutes = 0,
                    nextBookingStartsAt = null
                )
            }

            val nextBooking = windows.firstOrNull { !it.getStartAt().isBefore(startAt) }
            val maxAvailableMinutes = nextBooking?.let {
                Duration.between(startAt, it.getStartAt()).toMinutes().coerceAtLeast(0).toInt()
            }

            SeatMaxAvailabilityResponse(
                seatId = seatId,
                label = seat.label,
                type = seat.type,
                isAvailableAtStart = true,
                maxAvailableMinutes = maxAvailableMinutes,
                nextBookingStartsAt = nextBooking?.getStartAt()
            )
        }
    }
}
