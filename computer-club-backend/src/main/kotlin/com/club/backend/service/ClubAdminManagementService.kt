package com.club.backend.service

import com.club.backend.domain.entity.*
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ClubAdminManagementService(
    private val clubRepository: ClubRepository,
    private val userRepository: UserRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val auditService: AuditService
) {

    @Transactional(readOnly = true)
    fun listStaff(clubId: Long): List<ClubStaffView> {
        clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        return clubStaffRepository.findAllByIdClubId(clubId)
            .sortedBy { it.role.name }
            .map { it.toView() }
    }

    /** Добавить/обновить ADMIN. OWNER не трогаем через этот метод. */
    @Transactional
    fun upsertAdmin(clubId: Long, userId: Long, addedByUserId: Long): ClubStaffView {
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club not found") }
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        val addedBy = userRepository.findById(addedByUserId).orElse(null)

        val existing = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId).orElse(null)
        val saved = if (existing == null) {
            clubStaffRepository.save(
                ClubStaffEntity(
                    id = ClubStaffId(clubId = clubId, userId = userId),
                    club = club,
                    user = user,
                    role = ClubRole.ADMIN,
                    addedByUser = addedBy,
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

        auditService.log(
            actorUserId = addedByUserId,
            clubId = clubId,
            action = "STAFF_ADD",
            entityType = "ClubStaff",
            entityId = userId.toString(),
            after = mapOf("userId" to userId, "role" to "ADMIN")
        )
        return saved.toView()
    }

    @Transactional(readOnly = true)
    fun lookupByPhone(phone: String): ClubStaffView {
        val normalized = normalizePhone(phone)
        val user = userRepository.findByPhone(normalized)
            .orElseThrow { EntityNotFoundException("Пользователь с таким номером не найден") }
        return ClubStaffView(userId = user.id!!, phone = user.phone, role = user.globalRole.name, addedAt = null, addedByUserId = null, addedByPhone = null)
    }

    private fun normalizePhone(raw: String): String {
        val p = raw.trim().replace(" ", "").replace("-", "")
        return if (p.startsWith("+")) p else "+$p"
    }

    @Transactional
    fun removeAdmin(clubId: Long, userId: Long, actorUserId: Long) {
        val cs = clubStaffRepository.findByIdClubIdAndIdUserId(clubId, userId)
            .orElseThrow { EntityNotFoundException("Staff member not found") }
        require(cs.role != ClubRole.OWNER) { "Cannot remove club owner" }
        clubStaffRepository.delete(cs)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "STAFF_REMOVE",
            entityType = "ClubStaff",
            entityId = userId.toString(),
            before = mapOf("userId" to userId, "role" to cs.role.name)
        )
    }
}

data class ClubStaffView(
    val userId: Long,
    val phone: String?,
    val role: String,
    val addedAt: String?,
    val addedByUserId: Long?,
    val addedByPhone: String?
)

private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun ClubStaffEntity.toView() = ClubStaffView(
    userId = user.id!!,
    phone = user.phone,
    role = role.name,
    addedAt = createdAt.format(isoFormatter),
    addedByUserId = addedByUser?.id,
    addedByPhone = addedByUser?.phone
)
