package com.club.backend.service

import com.club.backend.domain.entity.ClubUserBlockEntity
import com.club.backend.domain.entity.ClubUserBlockId
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubUserBlockRepository
import com.club.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class ClubUserBlockView(
    val userId: Long,
    val phone: String?,
    val username: String,
    val isBlocked: Boolean,
    val reason: String?,
    val blockedUntil: LocalDateTime?
)

data class UpsertClubUserBlockRequest(
    val isBlocked: Boolean,
    val reason: String? = null,
    val blockedUntil: LocalDateTime? = null
)

@Service
class ClubUserBlockAdminService(
    private val clubRepository: ClubRepository,
    private val userRepository: UserRepository,
    private val clubUserBlockRepository: ClubUserBlockRepository,
    private val auditService: AuditService
) {

    @Transactional(readOnly = true)
    fun list(clubId: Long): List<ClubUserBlockView> {
        clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        return clubUserBlockRepository.findAllByIdClubId(clubId).map {
            ClubUserBlockView(
                userId = it.user.id!!,
                phone = it.user.phone,
                username = it.user.username,
                isBlocked = it.isBlocked,
                reason = it.reason,
                blockedUntil = it.blockedUntil
            )
        }
    }

    @Transactional
    fun upsert(actorUserId: Long, clubId: Long, userId: Long, req: UpsertClubUserBlockRequest): ClubUserBlockView {
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }

        val id = ClubUserBlockId(clubId = clubId, userId = userId)
        val existing = clubUserBlockRepository.findById(id).orElse(null)

        val saved = if (existing == null) {
            clubUserBlockRepository.save(
                ClubUserBlockEntity(
                    id = id,
                    club = club,
                    user = user,
                    isBlocked = req.isBlocked,
                    reason = req.reason,
                    blockedUntil = req.blockedUntil,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        } else {
            existing.isBlocked = req.isBlocked
            existing.reason = req.reason
            existing.blockedUntil = req.blockedUntil
            existing.updatedAt = LocalDateTime.now()
            clubUserBlockRepository.save(existing)
        }

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = if (saved.isBlocked) "CLUB_USER_BLOCK" else "CLUB_USER_UNBLOCK",
            entityType = "ClubUserBlock",
            entityId = "${clubId}:${userId}",
            before = existing?.let { mapOf("isBlocked" to it.isBlocked, "reason" to it.reason, "blockedUntil" to it.blockedUntil?.toString()) },
            after = mapOf("isBlocked" to saved.isBlocked, "reason" to saved.reason, "blockedUntil" to saved.blockedUntil?.toString())
        )

        return ClubUserBlockView(
            userId = user.id!!,
            phone = user.phone,
            username = user.username,
            isBlocked = saved.isBlocked,
            reason = saved.reason,
            blockedUntil = saved.blockedUntil
        )
    }
}
