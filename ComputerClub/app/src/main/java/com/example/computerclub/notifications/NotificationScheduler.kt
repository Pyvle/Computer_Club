package com.example.computerclub.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val REMINDER_MINUTES = 30L

    /** Планирует уведомление за 30 минут до начала брони. */
    fun scheduleBookingReminder(
        context: Context,
        bookingId: String,
        clubName: String,
        startAt: LocalDateTime
    ) {
        val reminderAt = startAt.minusMinutes(REMINDER_MINUTES)
        val delayMs = reminderAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - System.currentTimeMillis()

        // не планируем если уже прошло время напоминания
        if (delayMs <= 0) return

        val data = Data.Builder()
            .putString(BookingReminderWorker.KEY_CLUB_NAME, clubName)
            .putString(BookingReminderWorker.KEY_START_TIME, "%02d:%02d".format(startAt.hour, startAt.minute))
            .putInt(BookingReminderWorker.KEY_NOTIF_ID, bookingId.hashCode())
            .build()

        val request = OneTimeWorkRequestBuilder<BookingReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(bookingId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelBookingReminder(context: Context, bookingId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(bookingId))
    }

    private fun workName(bookingId: String) = "booking_reminder_$bookingId"
}
