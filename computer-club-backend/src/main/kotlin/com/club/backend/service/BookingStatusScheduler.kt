package com.club.backend.service

import com.club.backend.repository.BookingRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** Каждую минуту синхронизирует статусы броней с текущим временем. */
@Component
class BookingStatusScheduler(
    private val bookingRepository: BookingRepository
) {

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun tick() {
        val now = LocalDateTime.now()
        bookingRepository.activateStarted(now)
        bookingRepository.completeFinished(now)
    }
}
