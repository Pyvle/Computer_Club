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
    fun overlaps(other: TimeRange): Boolean =
        startMin < other.endMin && other.startMin < endMin
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

data class BookingDraft(
    val clubId: String,
    val date: LocalDate = LocalDate.now(),
    // минуты от 00:00
    val startMin: Int = 18 * 60,
    val endMin: Int = 19 * 60,
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