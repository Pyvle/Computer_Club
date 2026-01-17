package com.example.computerclub.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.*

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

    // Cart products
    var cartLines: List<CartProductLine> by mutableStateOf(emptyList())
        private set

    // Current session mock
    var currentSession: CurrentSessionSummary? by mutableStateOf(null)
        private set

    fun isLoggedIn(): Boolean = user != null

    fun setClub(clubId: String) {
        selectedClubId = clubId
        bookingDraft = bookingDraft.copy(clubId = clubId, selectedSeatIds = emptySet(), startMin = null, endMin = null)
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

    fun setTime(startMin: Int?, endMin: Int?) {
        bookingDraft = bookingDraft.copy(startMin = startMin, endMin = endMin)
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

        val start = bookingDraft.startMin ?: 12*60
        val end = bookingDraft.endMin ?: 13*60
        currentSession = CurrentSessionSummary(
            clubName = clubName,
            seatLabels = if (seats.isEmpty()) listOf("—") else seats,
            startLabel = minToLabel(start),
            endLabel = minToLabel(end),
            remainingLabel = "00:45 (мок)"
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
}
