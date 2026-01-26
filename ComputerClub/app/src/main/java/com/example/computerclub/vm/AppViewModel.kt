package com.example.computerclub.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
// Возможное продление текущей брони (мок): до 2 часов
private const val MAX_EXTENSION_MIN = 120

// Тарифы (мок)
private const val RATE_STANDARD_RUB_PER_HOUR = 100
private const val RATE_PKG_3H_RUB_PER_HOUR = 90
private const val RATE_PKG_5H_RUB_PER_HOUR = 80



class AppViewModel : ViewModel() {

    /**
     * Корзина хранится ПО КЛУБАМ, чтобы не смешивать брони/заказы разных клубов.
     * (В приложении есть ещё система заказов, поэтому общая корзина быстро создаёт путаницу.)
     */
    data class ClubCartState(
        val bookingLines: SnapshotStateList<CartBookingLine> = mutableStateListOf(),
        val productLines: SnapshotStateList<CartProductLine> = mutableStateListOf()
    )

    private val cartsByClub = mutableStateMapOf<String, ClubCartState>()

    private fun cartFor(clubId: String): ClubCartState =
        cartsByClub.getOrPut(clubId) { ClubCartState() }

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

    /**
     * Строки брони в корзине ТЕКУЩЕГО клуба.
     * Для другого клуба см. [bookingCartLinesFor].
     */
    val bookingCartLines: SnapshotStateList<CartBookingLine>
        get() = cartFor(selectedClubId).bookingLines

    fun bookingCartLinesFor(clubId: String): SnapshotStateList<CartBookingLine> = cartFor(clubId).bookingLines

    // Если пользователь редактирует бронь из корзины — тут хранится id строки.
    var editingBookingId: String? by mutableStateOf(null)
        private set

    // Чтобы при переходах по нижней панели "Бронь" возвращалась туда, где пользователь был в последний раз.
    var lastBookingRoute: String by mutableStateOf(Routes.Booking)
        private set

    /** Товары/услуги в корзине ТЕКУЩЕГО клуба. */
    val cartLines: SnapshotStateList<CartProductLine>
        get() = cartFor(selectedClubId).productLines

    fun cartLinesFor(clubId: String): SnapshotStateList<CartProductLine> = cartFor(clubId).productLines

    // Current session mock
    var currentSession: CurrentSessionSummary? by mutableStateOf(null)
        private set

    fun isLoggedIn(): Boolean = user != null

    fun setClub(clubId: String) {
        selectedClubId = clubId
        bookingDraft = BookingDraft(clubId = clubId)
        clubConfirmed = false
        editingBookingId = null
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
        val lines = cartFor(selectedClubId).productLines
        val idx = lines.indexOfFirst { it.productId == product.id && it.variant == variant }
        if (idx < 0) {
            lines.add(CartProductLine(product.id, product.title, product.price, variant, 1))
        } else {
            val cur = lines[idx]
            lines[idx] = cur.copy(qty = cur.qty + 1)
        }
    }

    fun changeQty(productId: String, variant: String?, delta: Int) {
        val lines = cartFor(selectedClubId).productLines
        val idx = lines.indexOfFirst { it.productId == productId && it.variant == variant }
        if (idx < 0) return
        val cur = lines[idx]
        val newQty = (cur.qty + delta).coerceAtLeast(0)
        if (newQty == 0) lines.removeAt(idx)
        else lines[idx] = cur.copy(qty = newQty)
    }

    /** Очистить корзину ОДНОГО клуба (по умолчанию — текущего). */
    fun clearCart(clubId: String = selectedClubId) {
        cartFor(clubId).productLines.clear()
        cartFor(clubId).bookingLines.clear()
        if (clubId == selectedClubId) {
            bookingDraft = BookingDraft(clubId = selectedClubId)
            editingBookingId = null
        }
    }

    /** Полная очистка всех корзин (если понадобится). */
    fun clearAllCarts() {
        cartsByClub.values.forEach {
            it.productLines.clear()
            it.bookingLines.clear()
        }
        cartsByClub.clear()
        bookingDraft = BookingDraft(clubId = selectedClubId)
        editingBookingId = null
    }

    // --- Edit booking in cart ---
    fun beginEditBooking(lineId: String): Boolean {
        // Ищем строку во всех корзинах (на всякий случай: id уникальные, но клуб может быть не текущим).
        val (clubId, line) = cartsByClub.entries
            .asSequence()
            .mapNotNull { e ->
                val found = e.value.bookingLines.firstOrNull { it.id == lineId }
                if (found == null) null else (e.key to found)
            }
            .firstOrNull() ?: return false
        // Переключаемся на клуб строки (чтобы экраны корректно подтянули места и т.п.)
        selectedClubId = clubId
        clubConfirmed = true
        editingBookingId = lineId
        bookingDraft = BookingDraft(
            clubId = line.clubId,
            date = line.date,
            startDayOffset = line.startDayOffset,
            startMin = line.startMin,
            endDayOffset = line.endDayOffset,
            endMin = line.endMin,
            packageHours = line.packageHours,
            selectedSeatIds = line.seatIds.toSet()
        )
        return true
    }

    fun cancelEditBooking() {
        editingBookingId = null
        // оставим текущий черновик как есть — пользователь может продолжить собирать новую бронь
    }

    data class CommitResult(val ok: Boolean, val message: String? = null)

    /**
     * Добавить текущую выбранную бронь в корзину.
     * В корзину попадает ТОЛЬКО когда выбраны места (или через быструю бронь).
     */
    fun addCurrentBookingToCart(): Boolean = commitCurrentBookingToCart().ok

    /**
     * Коммит брони в корзину:
     * - если editingBookingId != null => обновляем строку
     * - иначе добавляем новую
     * - не допускаем повторов: один и тот же компьютер не может быть забронирован
     *   пользователем в пересекающийся интервал (даже повторно “самому себе”).
     */
    fun commitCurrentBookingToCart(): CommitResult {
        val d = bookingDraft
        if (d.selectedSeatIds.isEmpty()) return CommitResult(false, "Выбери места")

        val start = startDateTime(d)
        val end = endDateTime(d)
        if (!start.isBefore(end)) return CommitResult(false, "Некорректное время")

        val exclude = editingBookingId

        // 1) Проверка конфликтов по каждому месту
        val bookingLines = cartFor(d.clubId).bookingLines

        val conflictSeat = d.selectedSeatIds.firstOrNull { seatId ->
            bookingLines.any { line ->
                if (line.id == exclude) return@any false
                if (line.clubId != d.clubId) return@any false
                if (!line.seatIds.contains(seatId)) return@any false
                val ls = lineStartDateTime(line)
                val le = lineEndDateTime(line)
                intervalsOverlap(start, end, ls, le)
            }
        }
        if (conflictSeat != null) {
            return CommitResult(false, "Это место уже есть в корзине на это время")
        }

        // 2) Проверка полного дубля (все поля совпадают)
        val normalizedSeatIds = d.selectedSeatIds.toList().sorted()
        val isExactDuplicate = bookingLines.any { line ->
            if (line.id == exclude) return@any false
            line.clubId == d.clubId &&
                    line.date == d.date &&
                    line.startDayOffset == d.startDayOffset && line.startMin == d.startMin &&
                    line.endDayOffset == d.endDayOffset && line.endMin == d.endMin &&
                    line.packageHours == d.packageHours &&
                    line.seatIds.sorted() == normalizedSeatIds
        }
        if (isExactDuplicate) {
            return CommitResult(false, "Такая бронь уже добавлена")
        }

        if (exclude != null) {
            val idx = bookingLines.indexOfFirst { it.id == exclude }
            if (idx >= 0) {
                bookingLines[idx] = CartBookingLine(
                    id = exclude,
                    clubId = d.clubId,
                    date = d.date,
                    startDayOffset = d.startDayOffset,
                    startMin = d.startMin,
                    endDayOffset = d.endDayOffset,
                    endMin = d.endMin,
                    packageHours = d.packageHours,
                    seatIds = normalizedSeatIds
                )
            }
            editingBookingId = null
        } else {
            val id = "b-${System.currentTimeMillis()}-${bookingLines.size}"
            bookingLines.add(
                CartBookingLine(
                    id = id,
                    clubId = d.clubId,
                    date = d.date,
                    startDayOffset = d.startDayOffset,
                    startMin = d.startMin,
                    endDayOffset = d.endDayOffset,
                    endMin = d.endMin,
                    packageHours = d.packageHours,
                    seatIds = normalizedSeatIds
                )
            )
        }

        // очищаем выбор мест в черновике, чтобы пользователь мог собрать следующую бронь
        bookingDraft = d.copy(selectedSeatIds = emptySet())
        return CommitResult(true)
    }

    fun removeBookingFromCart(id: String) {
        cartsByClub.values.forEach { it.bookingLines.removeAll { line -> line.id == id } }
        if (editingBookingId == id) editingBookingId = null
    }

    fun bookingLineCost(line: CartBookingLine): Int {
        val startAbs = line.startDayOffset * DAY_MIN + line.startMin
        val endAbs = line.endDayOffset * DAY_MIN + line.endMin
        val minutes = endAbs - startAbs
        if (minutes <= 0) return 0
        val hours = (minutes + 59) / 60 // округление вверх
        val rate = bookingRateRubPerHour(line.packageHours)
        return rate * hours * line.seatIds.size
    }

    fun cartBookingsTotal(clubId: String = selectedClubId): Int =
        cartFor(clubId).bookingLines.sumOf { bookingLineCost(it) }

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
        // Ручной выбор конца: пакет сохраняем ТОЛЬКО если конец строго соответствует пакету.
        val pkg = d.packageHours
        val keepPkg = pkg != null && Duration.between(start, newEnd).toMinutes().toInt() == pkg * 60
        bookingDraft = packDraft(d.copy(packageHours = if (keepPkg) pkg else null), start, newEnd)
    }

    /**
     * Применить пакет времени (в часах):
     * - длительность становится размером пакета
     * - конец автоматически = начало + пакет
     */
    fun applyTimePackage(hours: Int) {
        val d = bookingDraft
        val startAbs = d.startDayOffset * DAY_MIN + d.startMin
        val newEndAbs = startAbs + (hours * 60)
        bookingDraft = d.copy(
            packageHours = hours,
            endDayOffset = Math.floorDiv(newEndAbs, DAY_MIN),
            endMin = ((newEndAbs % DAY_MIN) + DAY_MIN) % DAY_MIN
        )
    }

    fun toggleSeat(seatId: String) {
        val set = bookingDraft.selectedSeatIds.toMutableSet()
        if (set.contains(seatId)) set.remove(seatId) else set.add(seatId)
        bookingDraft = bookingDraft.copy(selectedSeatIds = set)
    }

    /**
     * Быстрая бронь (пока на 1 человека):
     * - выбирает первый доступный компьютер на выбранный интервал
     * - кладёт его в bookingDraft.selectedSeatIds (ровно один)
     * Возвращает true, если место найдено.
     */
    fun quickBookOneSeat(): Boolean {
        val d = bookingDraft
        val range = selectedTimeRangeForSeats(d)
        val start = startDateTime(d)
        val end = endDateTime(d)

        val seats = FakeData.seatMapByClub[d.clubId].orEmpty()
            .filter { it.hasPc }

        val free = seats.firstOrNull { seatAvailability(it, range, start, end) == SeatAvailability.FREE }
            ?: return false

        bookingDraft = d.copy(selectedSeatIds = setOf(free.id))
        return true
    }

    private fun lineStartDateTime(line: CartBookingLine): LocalDateTime =
        line.date.plusDays(line.startDayOffset.toLong()).atStartOfDay().plusMinutes(line.startMin.toLong())

    private fun lineEndDateTime(line: CartBookingLine): LocalDateTime =
        line.date.plusDays(line.endDayOffset.toLong()).atStartOfDay().plusMinutes(line.endMin.toLong())

    private fun intervalsOverlap(aStart: LocalDateTime, aEnd: LocalDateTime, bStart: LocalDateTime, bEnd: LocalDateTime): Boolean {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd)
    }

    /**
     * Доступность места с учётом:
     * - мок-броней FakeData (внутри суток)
     * - риска продления (+2 часа)
     * - броней пользователя в корзине (пересечение по реальной дате/времени)
     */
    fun seatAvailability(seat: Seat, selected: TimeRange?, startDt: LocalDateTime, endDt: LocalDateTime): SeatAvailability {
        // 0) Приватный конфликт (уже добавлено в корзину пользователем)
        val exclude = editingBookingId
        val hasCartConflict = cartFor(bookingDraft.clubId).bookingLines.any { line ->
            if (line.id == exclude) return@any false
            if (line.clubId != bookingDraft.clubId) return@any false
            if (!line.seatIds.contains(seat.id)) return@any false
            intervalsOverlap(startDt, endDt, lineStartDateTime(line), lineEndDateTime(line))
        }
        if (hasCartConflict) return SeatAvailability.BOOKED

        if (selected == null) return SeatAvailability.FREE

        fun segments(r: TimeRange): List<Pair<Int, Int>> {
            return if (r.endMin >= r.startMin) listOf(r.startMin to r.endMin)
            else listOf(r.startMin to DAY_MIN, 0 to r.endMin)
        }

        fun overlaps(a: List<Pair<Int, Int>>, b: List<Pair<Int, Int>>): Boolean {
            return a.any { (as0, ae) -> b.any { (bs, be) -> as0 < be && bs < ae } }
        }

        val selSeg = segments(selected)
        val bookedSeg = seat.booked.flatMap(::segments)

        // 1) Прямое пересечение с бронью
        if (overlaps(selSeg, bookedSeg)) {
            // “частично” — если пересекается, но не полностью покрывает выбранный диапазон (упрощённо)
            // Для wrap-диапазонов оставляем PARTIAL.
            val fullyCovered = seat.booked.any { it.startMin <= selected.startMin && selected.endMin <= it.endMin }
            return if (fullyCovered) SeatAvailability.BOOKED else SeatAvailability.PARTIAL
        }

        // 2) Риск продления: считаем, что любая бронь может продлиться до +2 часов
        // => если выбранный диапазон попадает в расширенный интервал, место блокируем.
        val extendedBookedSeg = seat.booked
            .flatMap { r ->
                segments(r).flatMap { (s, e) ->
                    val e2 = e + MAX_EXTENSION_MIN
                    if (e2 <= DAY_MIN) listOf(s to e2)
                    else listOf(s to DAY_MIN, 0 to (e2 - DAY_MIN))
                }
            }

        if (overlaps(selSeg, extendedBookedSeg)) return SeatAvailability.BOOKED

        return SeatAvailability.FREE
    }

    fun checkoutByWallet(total: Int): Boolean {
        if (!isLoggedIn()) return false
        if (balance < total) return false
        balance -= total
        val clubId = selectedClubId
        mockStartSession(cartFor(clubId).bookingLines.lastOrNull())
        clearCart(clubId)
        return true
    }

    fun checkoutByCard(total: Int): Boolean {
        // мок-карта всегда “успешно”
        val clubId = selectedClubId
        mockStartSession(cartFor(clubId).bookingLines.lastOrNull())
        clearCart(clubId)
        return true
    }

    private fun mockStartSession(line: CartBookingLine?) {
        val usedClubId = line?.clubId ?: selectedClubId
        val clubName = FakeData.clubs.first { it.id == usedClubId }.name
        val seats = FakeData.seatMapByClub[usedClubId].orEmpty()
            .filter { s -> line?.seatIds?.contains(s.id) == true }
            .map { it.label }

        val start = line?.startMin ?: bookingDraft.startMin
        val end = line?.endMin ?: bookingDraft.endMin
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

    fun cartTotal(clubId: String = selectedClubId): Int =
        cartFor(clubId).productLines.sumOf { it.price * it.qty }

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
        ).normalizePackageIfNeeded()
    }

    fun setEndAbsolute(newAbs: Int) {
        val d = bookingDraft
        val startAbs = d.startDayOffset * DAY_MIN + d.startMin

        val oldEndAbs = d.endDayOffset * DAY_MIN + d.endMin
        if (newAbs == oldEndAbs) return

        if (newAbs < startAbs + 60) return

        val pkg = d.packageHours
        val keepPkg = pkg != null && newAbs == startAbs + (pkg * 60)

        // Пакет НЕ сбрасываем, если новый конец равен (начало + пакет).
        bookingDraft = d.copy(
            packageHours = if (keepPkg) pkg else null,
            endDayOffset = Math.floorDiv(newAbs, DAY_MIN),
            endMin = ((newAbs % DAY_MIN) + DAY_MIN) % DAY_MIN
        ).normalizePackageIfNeeded()
    }

    /**
     * Тариф (₽/ч) по выбранному пакету.
     * null => стандартный тариф.
     */
    fun bookingRateRubPerHour(packageHours: Int?): Int {
        return when (packageHours) {
            3 -> RATE_PKG_3H_RUB_PER_HOUR
            5 -> RATE_PKG_5H_RUB_PER_HOUR
            else -> RATE_STANDARD_RUB_PER_HOUR
        }
    }

    /** Удобный оверлоад для текущего черновика. */
    fun bookingRateRubPerHour(d: BookingDraft = bookingDraft): Int =
        bookingRateRubPerHour(d.packageHours)

    private fun BookingDraft.normalizePackageIfNeeded(): BookingDraft {
        val pkg = packageHours ?: return this
        val startAbs = startDayOffset * DAY_MIN + startMin
        val endAbs = endDayOffset * DAY_MIN + endMin
        val dur = endAbs - startAbs
        return if (dur == pkg * 60) this else copy(packageHours = null)
    }
}
