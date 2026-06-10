package com.club.backend.service

import com.club.backend.api.dto.CreateReportRequest
import com.club.backend.api.dto.UpdateReportStatusRequest
import com.club.backend.domain.entity.ClubEntity
import com.club.backend.domain.entity.ClubMessageEntity
import com.club.backend.domain.entity.ClubMessageType
import com.club.backend.domain.entity.UserEntity
import com.club.backend.domain.enum.ClubReportStatus
import com.club.backend.repository.ClubMessageRepository
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.UserRepository
import java.time.Instant
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class ClubUserReportServiceTest {

    private val clubs = mutableMapOf<Long, ClubEntity>()
    private val users = mutableMapOf<Long, UserEntity>()
    private val messages = linkedMapOf<Long, ClubMessageEntity>()
    private var nextMessageId = 1L

    private val messageRepo: ClubMessageRepository = proxyRepo { method, args ->
        when (method) {
            "save" -> {
                val entity = args[0] as ClubMessageEntity
                if (entity.id == null) entity.id = nextMessageId++
                messages[entity.id!!] = entity
                entity
            }
            "findById" -> Optional.ofNullable(messages[args[0] as Long])
            "findAllByClub_IdAndMessageTypeOrderByCreatedAtDesc" -> {
                val clubId = args[0] as Long
                val type = args[1] as ClubMessageType
                messages.values.filter { it.club.id == clubId && it.messageType == type }.sortedByDescending { it.createdAt }
            }
            "findAllByClub_IdAndMessageTypeAndStatusOrderByCreatedAtDesc" -> {
                val clubId = args[0] as Long
                val type = args[1] as ClubMessageType
                val status = args[2] as ClubReportStatus
                messages.values
                    .filter { it.club.id == clubId && it.messageType == type && it.status == status }
                    .sortedByDescending { it.createdAt }
            }
            else -> unsupported(method)
        }
    }
    private val clubRepo: ClubRepository = proxyRepo { method, args ->
        when (method) {
            "findById" -> Optional.ofNullable(clubs[args[0] as Long])
            else -> unsupported(method)
        }
    }
    private val userRepo: UserRepository = proxyRepo { method, args ->
        when (method) {
            "findById" -> Optional.ofNullable(users[args[0] as Long])
            else -> unsupported(method)
        }
    }
    private val service = ClubUserReportService(messageRepo, clubRepo, userRepo)

    @Test
    fun `submit creates new user report with NEW status`() {
        val club = ClubEntity(id = 3L, name = "Arena", addressFull = "Москва, 1", addressShort = "1")
        val user = UserEntity(id = 15L, phone = "+79990000000")
        clubs[3L] = club
        users[15L] = user

        service.submit(3L, 15L, CreateReportRequest(message = "Не работает ПК"))

        val saved = messages.values.single()
        assertEquals(club, saved.club)
        assertEquals(user, saved.author)
        assertEquals(ClubMessageType.USER_REPORT, saved.messageType)
        assertEquals("Не работает ПК", saved.message)
        assertEquals(ClubReportStatus.NEW, saved.status)
    }

    @Test
    fun `updateStatus changes report status and returns updated response`() {
        val club = ClubEntity(id = 9L, name = "Arena", addressFull = "Москва, 1", addressShort = "1")
        val author = UserEntity(id = 22L, phone = "+79991112233")
        val report = ClubMessageEntity(
            id = 5L,
            club = club,
            author = author,
            messageType = ClubMessageType.USER_REPORT,
            message = "Нужна уборка",
            status = ClubReportStatus.NEW,
            createdAt = Instant.parse("2026-05-15T10:15:30Z")
        )
        messages[5L] = report

        val response = service.updateStatus(9L, 5L, UpdateReportStatusRequest(ClubReportStatus.RESOLVED))

        assertEquals(ClubReportStatus.RESOLVED, report.status)
        assertEquals(5L, response.id)
        assertEquals(22L, response.userId)
        assertEquals(ClubReportStatus.RESOLVED, response.status)
    }

    @Test
    fun `getPlatformMessages maps warnings for club owner area`() {
        val club = ClubEntity(id = 4L, name = "Arena", addressFull = "Москва, 1", addressShort = "1")
        messages[11L] = ClubMessageEntity(
            id = 11L,
            club = club,
            messageType = ClubMessageType.PLATFORM_WARNING,
            message = "Обновите описание клуба",
            createdAt = Instant.parse("2026-05-15T10:15:30Z")
        )

        val result = service.getPlatformMessages(4L)

        assertEquals(1, result.size)
        assertEquals(11L, result.first().id)
        assertEquals("Обновите описание клуба", result.first().message)
    }
}
