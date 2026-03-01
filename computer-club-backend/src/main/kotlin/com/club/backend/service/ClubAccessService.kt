package com.club.backend.service

import com.club.backend.repository.ClubUserBlockRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import com.club.backend.api.error.BlockedInClubException

@Service
class ClubAccessService(
    private val clubUserBlockRepository: ClubUserBlockRepository
) {
    fun ensureNotBlocked(userId: Long, clubId: Long) {
        val blocked = clubUserBlockRepository.isBlockedNow(clubId, userId, LocalDateTime.now())
        if (blocked) throw BlockedInClubException()
    }
}
