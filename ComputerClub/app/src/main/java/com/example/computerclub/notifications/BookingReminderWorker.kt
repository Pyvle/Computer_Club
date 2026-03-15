package com.example.computerclub.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BookingReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val clubName = inputData.getString(KEY_CLUB_NAME) ?: "Клуб"
        val startTime = inputData.getString(KEY_START_TIME) ?: ""
        val notifId = inputData.getInt(KEY_NOTIF_ID, 0)

        NotificationHelper.postReminder(
            context = applicationContext,
            title = "Скоро начало брони — $clubName",
            text = "Бронирование начнётся в $startTime",
            notifId = notifId
        )

        return Result.success()
    }

    companion object {
        const val KEY_CLUB_NAME = "clubName"
        const val KEY_START_TIME = "startTime"
        const val KEY_NOTIF_ID = "notifId"
    }
}
