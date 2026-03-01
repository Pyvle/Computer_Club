package com.club.backend.service

import com.club.backend.domain.entity.*
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ClubAdminManagementService(
    private val clubRepository: ClubRepository,
    private val userRepository: UserRepository,
    private val clubStaffRepository: ClubStaffRepository
) {

    @Transactional(readOnly = true)
    fun listStaff(clubId: Long): List<ClubStaffView> {
        clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        return clubStaffRepository.findAllByIdClubId(clubId)
            .sortedBy { it.role.name }
            .map {
                ClubStaffView(
                    userId = it.user.id!!,
                    phone = it.user.phone,
                    username = it.user.username,
                    role = it.role.name
                )
            }
    }

    /** Добавить/обновить ADMIN. OWNER не трогаем через этот метод. */
    @Transactional
    fun upsertAdmin(clubId: Long, userId: Long): ClubStaffView {
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }

        val existing = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId).orElse(null)
        val saved = if (existing == null) {
            clubStaffRepository.save(
                ClubStaffEntity(
                    id = ClubStaffId(clubId = clubId, userId = userId),
                    club = club,
                    user = user,
                    role = ClubRole.ADMIN,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        } else {
            if (existing.role != ClubRole.OWNER) {
                existing.role = ClubRole.ADMIN
            }
            existing.updatedAt = LocalDateTime.now()
            clubStaffRepository.save(existing)
        }

        return ClubStaffView(userId = user.id!!, phone = user.phone, username = user.username, role = saved.role.name)
    }

    @Transactional
    fun removeAdmin(clubId: Long, userId: Long) {
        val cs = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId)
            .orElseThrow { EntityNotFoundException("Staff member not found") }
        require(cs.role != ClubRole.OWNER) { "Cannot remove club owner" }
        clubStaffRepository.delete(cs)
    }
}

data class ClubStaffView(
    val userId: Long,
    val phone: String,
    val username: String,
    val role: String
)
