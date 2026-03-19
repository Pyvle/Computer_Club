package com.club.backend.service

import com.club.backend.api.dto.admin.SeatSpecResponse
import com.club.backend.api.dto.admin.SpecLine
import com.club.backend.api.dto.admin.UpdateSeatSpecRequest
import com.club.backend.domain.entity.ClubSeatSpecEntity
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubSeatSpecRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubSeatSpecService(
    private val specRepo: ClubSeatSpecRepository,
    private val clubRepo: ClubRepository,
    private val mapper: ObjectMapper
) {

    private val specListType = object : TypeReference<List<SpecLine>>() {}

    fun getByClub(clubId: Long): List<SeatSpecResponse> =
        specRepo.findAllByClubId(clubId).map { it.toResponse() }

    @Transactional
    fun update(clubId: Long, seatType: String, req: UpdateSeatSpecRequest): SeatSpecResponse {
        val upper = seatType.uppercase()
        require(upper in setOf("REGULAR", "VIP")) { "Unknown seatType: $seatType" }

        val entity = specRepo.findByClubIdAndSeatType(clubId, upper)
            ?: run {
                val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
                ClubSeatSpecEntity(club = club, seatType = upper, title = req.title)
            }

        entity.title = req.title
        entity.specsJson = mapper.writeValueAsString(req.specs)
        return specRepo.save(entity).toResponse()
    }

    private fun ClubSeatSpecEntity.toResponse() = SeatSpecResponse(
        seatType = seatType,
        title = title,
        specs = runCatching { mapper.readValue(specsJson, specListType) }.getOrDefault(emptyList())
    )
}
