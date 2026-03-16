package com.example.computerclub.model

import java.time.LocalDate
import java.time.LocalDateTime

data class User(val id: String, val phone: String)

data class NewsItem(
    val id: String,
    val clubName: String,
    val title: String,
    val text: String
)

enum class SeatType { REGULAR, VIP }

data class TimeRange(val startMin: Int, val endMin: Int) {
    /**
     * Проверяет пересечение диапазонов внутри суток.
     * Поддерживает "wrap" (23:30–01:00) — разбивает на два сегмента.
     */
    fun overlaps(other: TimeRange): Boolean {
        fun segments(r: TimeRange): List<Pair<Int, Int>> {
            // обычный диапазон
            if (r.endMin >= r.startMin) return listOf(r.startMin to r.endMin)
            // wrap-диапазон (напр. 1410..60)
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
    val qty: Int,
    // id строки в серверной корзине (null для локальных/мок-строк)
    val lineId: Long? = null,
    // клуб, к которому относится строка (нужен для удаления/изменения при смене selectedClubId)
    val clubId: String? = null
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
    val date: LocalDate = LocalDate.now(),

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

data class Club(
    val id: String,
    val name: String,
    val location: String,
    val address: String,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null,

    // из /clubs/available
    val isBlocked: Boolean = false,
    val blockReason: String? = null
)

// --- Orders / History ---

enum class BookingStatus { UPCOMING, ACTIVE, DONE, CANCELED }

/** Снимок брони внутри оплаты (Purchase). */
data class BookingOrder(
    val id: String,
    val clubId: String,
    val clubName: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val seatIds: List<String>,
    val seatLabels: List<String>,
    val packageHours: Int?,
    val rateRubPerHour: Int,
    val totalRub: Int,
    val status: BookingStatus = BookingStatus.UPCOMING
)

enum class ProductOrderStatus { NOT_READY, READY, CANCELED }

enum class ReadyByPolicy { ASAP, BOOKING_START, CUSTOM }

data class ProductOrderItemSnapshot(
    val productId: String,
    val title: String,
    val variant: String?,
    val priceRub: Int,
    val qty: Int
)

/** Один заказ на товары/услуги в рамках оплаты (Purchase). */
data class ProductOrder(
    val id: String,
    val clubId: String,
    val clubName: String,
    val createdAt: LocalDateTime,
    val readyBy: LocalDateTime?,
    val readyByPolicy: ReadyByPolicy,
    val status: ProductOrderStatus,
    val items: List<ProductOrderItemSnapshot>,
    val totalRub: Int
)

/**
 * Одна оплата. Может содержать бронь и/или заказ товаров.
 * Это удобная модель под БД и админку.
 */
data class Purchase(
    val id: String,
    val clubId: String,
    val clubName: String,
    val createdAt: LocalDateTime,
    val bookingOrders: List<BookingOrder>,
    val productOrder: ProductOrder?,
    val bookingTotalRub: Int,
    val productsTotalRub: Int,
    val totalRub: Int,
    val paymentStatus: String = "PAID"
)
