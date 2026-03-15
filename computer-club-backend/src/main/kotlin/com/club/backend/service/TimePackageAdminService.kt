package com.club.backend.service

import com.club.backend.api.dto.admin.AdminTimePackageResponse
import com.club.backend.api.dto.admin.CreateTimePackageRequest
import com.club.backend.api.dto.admin.UpdateTimePackageRequest
import com.club.backend.domain.entity.ClubTimePackageEntity
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ClubTimePackageRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

@Service
class TimePackageAdminService(
    private val repo: ClubTimePackageRepository,
    private val clubRepository: ClubRepository,
    private val auditService: AuditService
) {

    fun list(clubId: Long): List<AdminTimePackageResponse> =
        repo.findAllByClub_IdOrderBySortOrderAscIdAsc(clubId).map { it.toDto() }

    @Transactional
    fun create(actorUserId: Long, clubId: Long, req: CreateTimePackageRequest): AdminTimePackageResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { EntityNotFoundException("Club $clubId not found") }
        val pkg = ClubTimePackageEntity(
            club = club,
            name = req.name.trim(),
            hours = req.hours,
            pricePerHourRub = req.pricePerHourRub,
            sortOrder = req.sortOrder,
            availableFrom = req.availableFrom?.let { LocalTime.parse(it, TIME_FMT) },
            availableTo = req.availableTo?.let { LocalTime.parse(it, TIME_FMT) }
        )
        val saved = repo.save(pkg)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "TIME_PACKAGE_CREATE",
            entityType = "TimePackage",
            entityId = saved.id!!.toString(),
            before = null,
            after = saved.toDto()
        )
        return saved.toDto()
    }

    @Transactional
    fun update(
        actorUserId: Long,
        clubId: Long,
        id: Long,
        req: UpdateTimePackageRequest
    ): AdminTimePackageResponse {
        val pkg = repo.findById(id).orElseThrow { EntityNotFoundException("TimePackage $id not found") }
        if (pkg.club.id != clubId) throw EntityNotFoundException("TimePackage $id not found for club $clubId")

        val before = pkg.toDto()
        pkg.name = req.name.trim()
        pkg.hours = req.hours
        pkg.pricePerHourRub = req.pricePerHourRub
        pkg.isActive = req.isActive
        pkg.sortOrder = req.sortOrder
        pkg.availableFrom = req.availableFrom?.let { LocalTime.parse(it, TIME_FMT) }
        pkg.availableTo = req.availableTo?.let { LocalTime.parse(it, TIME_FMT) }
        val saved = repo.save(pkg)

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "TIME_PACKAGE_UPDATE",
            entityType = "TimePackage",
            entityId = saved.id!!.toString(),
            before = before,
            after = saved.toDto()
        )
        return saved.toDto()
    }

    @Transactional
    fun delete(actorUserId: Long, clubId: Long, id: Long) {
        val pkg = repo.findById(id).orElseThrow { EntityNotFoundException("TimePackage $id not found") }
        if (pkg.club.id != clubId) throw EntityNotFoundException("TimePackage $id not found for club $clubId")

        val before = pkg.toDto()
        repo.delete(pkg)

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "TIME_PACKAGE_DELETE",
            entityType = "TimePackage",
            entityId = id.toString(),
            before = before,
            after = null
        )
    }

    private fun ClubTimePackageEntity.toDto() = AdminTimePackageResponse(
        id = requireNotNull(id),
        name = name,
        hours = hours,
        pricePerHourRub = pricePerHourRub,
        totalPriceRub = pricePerHourRub * hours,
        isActive = isActive,
        sortOrder = sortOrder,
        availableFrom = availableFrom?.format(TIME_FMT),
        availableTo = availableTo?.format(TIME_FMT)
    )
}
