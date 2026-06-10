package com.club.backend.service

import com.club.backend.api.dto.admin.SeatSpecResponse
import com.club.backend.api.dto.admin.SpecLine
import com.club.backend.api.dto.admin.UpdateSeatSpecRequest
import com.club.backend.domain.entity.ClubSeatTypeSettingEntity
import com.club.backend.domain.enum.SeatType
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubSeatTypeSettingRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClubSeatSpecService(
    private val settingRepo: ClubSeatTypeSettingRepository,
    private val clubRepo: ClubRepository,
    private val mapper: ObjectMapper
) {

    private val specListType = object : TypeReference<List<SpecLine>>() {}

    fun getByClub(clubId: Long): List<SeatSpecResponse> =
        settingRepo.findAllByClub_Id(clubId).map { it.toResponse() }

    @Transactional
    fun update(clubId: Long, seatType: String, req: UpdateSeatSpecRequest): SeatSpecResponse {
        val parsedSeatType = runCatching { SeatType.valueOf(seatType.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Unknown seatType: $seatType") }

        val entity = settingRepo.findByClub_IdAndSeatType(clubId, parsedSeatType).orElse(null)
            ?: run {
                val club = clubRepo.findById(clubId).orElseThrow { NoSuchElementException("Club $clubId not found") }
                ClubSeatTypeSettingEntity(club = club, seatType = parsedSeatType, title = req.title)
            }

        entity.title = req.title
        entity.specsJson = mapper.writeValueAsString(req.specs)
        return settingRepo.save(entity).toResponse()
    }

    private fun ClubSeatTypeSettingEntity.toResponse() = SeatSpecResponse(
        seatType = seatType.name,
        title = title,
        specs = runCatching { mapper.readValue(specsJson, specListType) }.getOrDefault(emptyList())
    )
}
