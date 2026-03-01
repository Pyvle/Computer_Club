package com.club.backend.service

import com.club.backend.domain.entity.*
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class SetGlobalRoleRequest(val role: String)

@Service
class GlobalAdminService(
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val clubStaffRepository: ClubStaffRepository
) {

    @Transactional
    fun setGlobalRole(userId: Long, role: String) {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val gr = runCatching { GlobalRole.valueOf(role) }.getOrElse { throw IllegalArgumentException("Unknown role") }
        user.globalRole = gr
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    /** Глобальный админ может назначить/сменить главу клуба. */
    @Transactional
    fun setClubOwner(clubId: Long, userId: Long) {
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }

        // демотим предыдущих OWNER (если были)
        clubStaffRepository.findAllByIdClubId(clubId).forEach {
            if (it.role == ClubRole.OWNER) {
                it.role = ClubRole.ADMIN
                it.updatedAt = LocalDateTime.now()
                clubStaffRepository.save(it)
            }
        }

        val existing = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId).orElse(null)
        if (existing == null) {
            clubStaffRepository.save(
                ClubStaffEntity(
                    id = ClubStaffId(clubId = clubId, userId = userId),
                    club = club,
                    user = user,
                    role = ClubRole.OWNER,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        } else {
            existing.role = ClubRole.OWNER
            existing.updatedAt = LocalDateTime.now()
            clubStaffRepository.save(existing)
        }
    }
}
