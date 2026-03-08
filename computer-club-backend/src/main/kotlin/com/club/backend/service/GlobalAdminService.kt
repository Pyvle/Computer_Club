package com.club.backend.service

import com.club.backend.api.dto.admin.AdminUserResponse
import com.club.backend.api.dto.admin.CreateUserRequest
import com.club.backend.domain.entity.*
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubStaffRepository
import com.club.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class SetGlobalRoleRequest(val role: String)

@Service
class GlobalAdminService(
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val clubStaffRepository: ClubStaffRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    fun listUsers(): List<AdminUserResponse> =
        userRepository.findAll().map { it.toDto() }

    @Transactional
    fun createUser(req: CreateUserRequest): AdminUserResponse {
        if (userRepository.findByPhone(req.phone).isPresent) {
            throw IllegalStateException("Пользователь с таким номером уже существует")
        }
        val role = runCatching { GlobalRole.valueOf(req.globalRole) }.getOrElse { throw IllegalArgumentException("Unknown role") }
        val user = userRepository.save(
            UserEntity(
                phone = req.phone,
                passwordHash = passwordEncoder.encode(req.password),
                globalRole = role
            )
        )
        return user.toDto()
    }

    @Transactional
    fun toggleUserActive(userId: Long, isActive: Boolean) {
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        user.isActive = isActive
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    @Transactional
    fun deleteUser(userId: Long, currentUserId: Long) {
        if (userId == currentUserId) throw IllegalStateException("Cannot delete yourself")
        userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }
        userRepository.deleteById(userId)
    }

    private fun UserEntity.toDto() = AdminUserResponse(
        id = id!!,
        phone = phone,
        isActive = isActive,
        globalRole = globalRole.name,
        hasPassword = passwordHash != null,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

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
