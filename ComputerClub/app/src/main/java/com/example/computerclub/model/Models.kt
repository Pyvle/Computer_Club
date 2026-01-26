package com.example.computerclub.model

import java.time.LocalDate

data class User(val id: String, val username: String, val phone: String)

data class NewsItem(
    val id: String,
    val clubName: String,
    val title: String,
    val text: String
)

enum class SeatType { REGULAR, VIP }

data class TimeRange(val startMin: Int, val endMin: Int) {
    /**
     * Overlap for ranges inside a day.
     * Supports "wrap" ranges (e.g. 23:30–01:00) by splitting into two segments.
     */
    fun overlaps(other: TimeRange): Boolean {
        fun segments(r: TimeRange): List<Pair<Int, Int>> {
            // normal
            if (r.endMin >= r.startMin) return listOf(r.startMin to r.endMin)
            // wrap (e.g. 1410..60)
            return listOf(r.startMin to 24 * 60, 0 to r.endMin)
        }

        val a = segments(this)
        val b = segments(other)
        return a.any { (as0, ae) -> b.any { (bs, be) -> as0 < be && bs < ae } }
    }
}

data class Seat(
    val id: String,
    val label: String,
    val type: SeatType,
    val hasPc: Boolean,
    val equipment: String,
    val booked: List<TimeRange> = emptyList(),
)

enum class SeatAvailability { FREE, BOOKED, PARTIAL }

data class ProductCategory(val id: String, val title: String)

data class Product(
    val id: String,
    val categoryId: String,
    val title: String,
    val price: Int,
    val description: String,
    val variants: List<String> = emptyList()
)

data class CartProductLine(
    val productId: String,
    val title: String,
    val price: Int,
    val variant: String?,
    val qty: Int
)

/**
 * Строка брони в корзине.
 * Это «снимок» выбранного времени/пакета/мест на момент добавления.
 * Пользователь может добавить несколько броней в корзину с разными тарифами.
 */
data class CartBookingLine(
    val id: String,
    val clubId: String,
    val date: LocalDate,
    val startDayOffset: Int,
    val startMin: Int,
    val endDayOffset: Int,
    val endMin: Int,
    val packageHours: Int?,
    val seatIds: List<String>
)

data class BookingDraft(
    val clubId: String,
    // Временная фиксация "текущей" даты для удобного тестирования (не читаем дату телефона).
    // Когда будешь готов привязать к реальному времени — верни LocalDate.now().
    val date: LocalDate = LocalDate.of(2026, 1, 25),

    val startDayOffset: Int = 0,
    val startMin: Int = 18 * 60,

    val endDayOffset: Int = 0,
    val endMin: Int = 19 * 60,

    /**
     * Выбранный пакет времени (в часах). null = обычная бронь.
     * Пока используем для расчёта тарифа в корзине.
     */
    val packageHours: Int? = null,

    val selectedSeatIds: Set<String> = emptySet()
)

data class CurrentSessionSummary(
    val clubName: String,
    val seatLabels: List<String>,
    val startLabel: String,
    val endLabel: String,
    val remainingLabel: String
)

data class AppNotification(
    val id: String,
    val title: String,
    val text: String
)

data class Club(
    val id: String,
    val name: String,
    val location: String,
    val address: String,
    val description: String
)
