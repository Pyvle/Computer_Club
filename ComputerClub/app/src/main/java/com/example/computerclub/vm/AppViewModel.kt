package com.example.computerclub.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.*
import com.example.computerclub.app.Routes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalTime

private const val DAY_MIN = 24 * 60
private const val STEP_MIN = 30
private const val MIN_GAP_MIN = 60L



class AppViewModel : ViewModel() {

    // Auth / Profile
    var user: User? by mutableStateOf(null)
        private set

    var balance: Int by mutableStateOf(0)
        private set

    // Shared club selection (Booking + Shop)
    var selectedClubId: String by mutableStateOf(FakeData.clubs.first().id)
        private set

    var clubConfirmed: Boolean by mutableStateOf(false)
        private set

    // Booking draft (selection before going to cart)
    var bookingDraft: BookingDraft by mutableStateOf(BookingDraft(clubId = selectedClubId))
        private set

    // Чтобы при переходах по нижней панели "Бронь" возвращалась туда, где пользователь был в последний раз.
    var lastBookingRoute: String by mutableStateOf(Routes.Booking)
        private set

    // Cart products
    var cartLines: List<CartProductLine> by mutableStateOf(emptyList())
        private set

    // Current session mock
    var currentSession: CurrentSessionSummary? by mutableStateOf(null)
        private set

    fun isLoggedIn(): Boolean = user != null

    fun setClub(clubId: String) {
        selectedClubId = clubId
        bookingDraft = BookingDraft(clubId = clubId)
        clubConfirmed = false
    }

    fun confirmClub() { clubConfirmed = true }

    fun login(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) return false
        user = User("u1", username.trim(), phone = "+7 (900) 000-00-00")
        if (balance == 0) balance = 500 // стартовый мок
        return true
    }

    fun register(phone: String, username: String, pass1: String, pass2: String, code: String): Boolean {
        if (phone.isBlank() || username.isBlank() || pass1.isBlank() || pass1 != pass2) return false
        if (code.trim() != "1234") return false // мок-код
        user = User("u1", username.trim(), phone.trim())
        if (balance == 0) balance = 500
        return true
    }

    fun logout() {
        user = null
        // можно оставить баланс как “кошелёк устройства”, но чаще обнуляют:
        // balance = 0
    }

    fun topUp(amount: Int): Boolean {
        if (!isLoggedIn()) return false
        if (amount <= 0) return false
        balance += amount
        return true
    }

    fun addProduct(product: Product, variant: String?) {
        val existing = cartLines.firstOrNull { it.productId == product.id && it.variant == variant }
        cartLines = if (existing == null) {
            cartLines + CartProductLine(product.id, product.title, product.price, variant, 1)
        } else {
            cartLines.map {
                if (it.productId == product.id && it.variant == variant) it.copy(qty = it.qty + 1) else it
            }
        }
    }

    fun changeQty(productId: String, variant: String?, delta: Int) {
        cartLines = cartLines.mapNotNull { line ->
            if (line.productId == productId && line.variant == variant) {
                val newQty = (line.qty + delta).coerceAtLeast(0)
                if (newQty == 0) null else line.copy(qty = newQty)
            } else line
        }
    }

    fun clearCart() {
        cartLines = emptyList()
        bookingDraft = BookingDraft(clubId = selectedClubId)
    }

    // --- Booking setup ---
    fun setBookingDate(date: LocalDate) {
        val d = bookingDraft
        val oldStart = startDateTime(d)
        val oldEnd = endDateTime(d)

        val newStart = LocalDateTime.of(date, LocalTime.of(oldStart.hour, oldStart.minute))
        // сдвигаем конец на ту же дельту по времени, чтобы длительность сохранилась
        val delta = Duration.between(oldStart, newStart)
        val newEnd = oldEnd.plus(delta)

        bookingDraft = packDraft(d.copy(date = date), newStart, newEnd)
    }

    /**
     * Выбор начала как (дата, минуты).
     * Конец двигается на ту же величину прокрутки, чтобы длительность сохранялась.
     */
    fun setStartSelection(date: LocalDate, startMin: Int) {
        val d = bookingDraft
        val oldStart = startDateTime(d)
        val oldEnd = endDateTime(d)
        val newStart = date.atStartOfDay().plusMinutes(startMin.toLong())
        val delta = Duration.between(oldStart, newStart)
        val newEnd = oldEnd.plus(delta)

        bookingDraft = packDraft(d.copy(date = date, startMin = startMin), newStart, newEnd)
    }

    /**
     * Выбор конца как (дата, минуты) — никак не влияет на начало.
     * Но не даём выбрать меньше (начало + 1 час).
     */
    fun setEndSelection(endDate: LocalDate, endMin: Int) {
        val d = bookingDraft
        val start = startDateTime(d)
        val picked = endDate.atStartOfDay().plusMinutes(endMin.toLong())
        val minEnd = start.plusMinutes(MIN_GAP_MIN)
        val newEnd = if (picked.isBefore(minEnd)) minEnd else picked
        bookingDraft = packDraft(d, start, newEnd)
    }

    fun toggleSeat(seatId: String) {
        val set = bookingDraft.selectedSeatIds.toMutableSet()
        if (set.contains(seatId)) set.remove(seatId) else set.add(seatId)
        bookingDraft = bookingDraft.copy(selectedSeatIds = set)
    }

    fun seatAvailability(seat: Seat, selected: TimeRange?): SeatAvailability {
        if (selected == null) return SeatAvailability.FREE
        val overlaps = seat.booked.any { it.overlaps(selected) }
        if (!overlaps) return SeatAvailability.FREE

        // “частично” — если пересекается, но не полностью покрывает выбранный диапазон (упрощённо)
        val fullyCovered = seat.booked.any { it.startMin <= selected.startMin && selected.endMin <= it.endMin }
        return if (fullyCovered) SeatAvailability.BOOKED else SeatAvailability.PARTIAL
    }

    fun checkoutByWallet(total: Int): Boolean {
        if (!isLoggedIn()) return false
        if (balance < total) return false
        balance -= total
        mockStartSession()
        clearCart()
        return true
    }

    fun checkoutByCard(total: Int): Boolean {
        // мок-карта всегда “успешно”
        mockStartSession()
        clearCart()
        return true
    }

    private fun mockStartSession() {
        val clubName = FakeData.clubs.first { it.id == selectedClubId }.name
        val seats = FakeData.seatMapByClub[selectedClubId].orEmpty()
            .filter { bookingDraft.selectedSeatIds.contains(it.id) }
            .map { it.label }

        val start = bookingDraft.startMin
        val end = bookingDraft.endMin
        currentSession = CurrentSessionSummary(
            clubName = clubName,
            seatLabels = if (seats.isEmpty()) listOf("—") else seats,
            startLabel = minToLabel(start),
            endLabel = minToLabel(end),
            remainingLabel = "00:45 (мок)"
        )
    }

    // Публичные хелперы для экранов (BookingSetup/BookingSeats)
    fun setEndMin(endMin: Int) =
        setEndSelection(bookingDraft.date.plusDays(bookingDraft.endDayOffset.toLong()), endMin)

    fun startDateTime(d: BookingDraft): LocalDateTime =
        d.date.plusDays(d.startDayOffset.toLong()).atStartOfDay().plusMinutes(d.startMin.toLong())

    fun endDateTime(d: BookingDraft): LocalDateTime =
        d.date.plusDays(d.endDayOffset.toLong()).atStartOfDay().plusMinutes(d.endMin.toLong())

    /**
     * Диапазон времени для подсветки занятости мест.
     * FakeData хранит брони без привязки к дате, поэтому:
     * - если длительность >= 24 часа, считаем что выбран "весь день"
     * - иначе возвращаем TimeRange(startMin,endMin) (может быть "оборачивающим" через 00:00)
     */
    fun selectedTimeRangeForSeats(d: BookingDraft): TimeRange? {
        val start = startDateTime(d)
        val end = endDateTime(d)
        val minutes = Duration.between(start, end).toMinutes()
        if (minutes <= 0) return null
        return if (minutes >= DAY_MIN.toLong()) {
            TimeRange(0, DAY_MIN)
        } else {
            TimeRange(d.startMin, d.endMin)
        }
    }

    /**
     * Приводит (start,end) к BookingDraft:
     * - end >= start + 1 час
     * - вычисляет endOffsetDays/endMin
     */
    private fun packDraft(base: BookingDraft, start: LocalDateTime, endRaw: LocalDateTime): BookingDraft {
        val minEnd = start.plusMinutes(MIN_GAP_MIN)
        val end = if (endRaw.isBefore(minEnd)) minEnd else endRaw

        val startDate = start.toLocalDate()
        val endDate = end.toLocalDate()
        val offsetDays =
            Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay())
                .toDays()
                .toInt()
                .coerceAtLeast(0)

        val startMin = start.toLocalTime().hour * 60 + start.toLocalTime().minute
        val endMin = end.toLocalTime().hour * 60 + end.toLocalTime().minute

        return base.copy(
            date = startDate,
            startDayOffset = 0,
            startMin = startMin % DAY_MIN,
            endDayOffset = offsetDays,
            endMin = endMin % DAY_MIN
        )
    }


    fun minToLabel(m: Int): String {
        val hh = (m / 60).toString().padStart(2, '0')
        val mm = (m % 60).toString().padStart(2, '0')
        return "$hh:$mm"
    }

    fun cartTotal(): Int = cartLines.sumOf { it.price * it.qty }

    val favoriteClubIds = mutableStateListOf<String>()

    fun isFavoriteClub(clubId: String): Boolean = favoriteClubIds.contains(clubId)

    fun toggleFavoriteClub(clubId: String) {
        if (favoriteClubIds.contains(clubId)) favoriteClubIds.remove(clubId)
        else favoriteClubIds.add(clubId)
    }

    fun chooseClub(clubId: String) {
        setClub(clubId)
        confirmClub()
    }

    fun onRouteChanged(route: String) {
        // Нормализуем для стабильности (без аргументов)
        val r = route.substringBefore("?")
        when {
            r.startsWith(Routes.BookingSeats) -> lastBookingRoute = Routes.BookingSeats
            r.startsWith(Routes.Booking) -> lastBookingRoute = Routes.Booking
        }
    }

    fun shiftStartBy(deltaMin: Int) {
        val d = bookingDraft

        val startAbs = d.startDayOffset * DAY_MIN + d.startMin
        val endAbs = d.endDayOffset * DAY_MIN + d.endMin

        val newStartAbs = startAbs + deltaMin
        val newEndAbs = endAbs + deltaMin

        bookingDraft = d.copy(
            startDayOffset = Math.floorDiv(newStartAbs, DAY_MIN),
            startMin = ((newStartAbs % DAY_MIN) + DAY_MIN) % DAY_MIN,
            endDayOffset = Math.floorDiv(newEndAbs, DAY_MIN),
            endMin = ((newEndAbs % DAY_MIN) + DAY_MIN) % DAY_MIN
        )
    }

    fun setEndAbsolute(newAbs: Int) {
        val d = bookingDraft
        val startAbs = d.startDayOffset * DAY_MIN + d.startMin

        if (newAbs < startAbs + 60) return

        bookingDraft = d.copy(
            endDayOffset = Math.floorDiv(newAbs, DAY_MIN),
            endMin = ((newAbs % DAY_MIN) + DAY_MIN) % DAY_MIN
        )
    }
}
