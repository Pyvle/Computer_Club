package com.example.computerclub.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.*
import com.example.computerclub.app.Routes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalTime
import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.computerclub.data.local.TokenStore
import com.example.computerclub.data.network.NetworkClient
import com.example.computerclub.data.repository.AuthRepository
import kotlinx.coroutines.launch
import com.example.computerclub.data.repository.ClubsRepository
import com.example.computerclub.data.repository.CatalogRepository
import com.example.computerclub.data.repository.CartRepository
import com.example.computerclub.data.repository.CheckoutRepository
import com.example.computerclub.data.repository.SeatRepository
import com.example.computerclub.data.repository.FloorplanRepository
import com.example.computerclub.data.network.dto.CartResponseDto
import com.example.computerclub.data.network.dto.PurchaseDetailsDto
import retrofit2.HttpException
import java.util.UUID
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val DAY_MIN = 24 * 60
private const val STEP_MIN = 30
private const val MIN_GAP_MIN = 60L
// Возможное продление текущей брони (мок): до 2 часов
private const val MAX_EXTENSION_MIN = 120

// Тарифы (мок)
private const val RATE_STANDARD_RUB_PER_HOUR = 100
private const val RATE_PKG_3H_RUB_PER_HOUR = 90
private const val RATE_PKG_5H_RUB_PER_HOUR = 80



class AppViewModel(app: Application) : AndroidViewModel(app) {

    // Не зависим от BuildConfig (часто ломается при изменении namespace/applicationId)
    private val isDebuggable: Boolean =
        (getApplication<Application>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val network = NetworkClient(
        context = getApplication(),
        baseUrl = "http://10.0.2.2:8080", // для Android Emulator; поменяй на свой хост
        debug = isDebuggable
    )

    private val authRepo = AuthRepository(
        api = network.authApi,
        tokenStore = TokenStore(getApplication())
    )

    private val clubsRepo = ClubsRepository(network.clubsApi)

    private val catalogRepo = CatalogRepository(network.productApi)
    private val cartRepo = CartRepository(network.cartApi)
    private val checkoutRepo = CheckoutRepository(network.checkoutApi)
    private val seatRepo = SeatRepository(network.seatApi)
    private val floorplanRepo = FloorplanRepository(network.floorplanApi)

    private fun selectedClubIdLongOrNull(): Long? =
        selectedClubId.toLongOrNull() ?: selectedClubId.removePrefix("c").toLongOrNull()

    fun loadMe(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (user != null) {
                onSuccess()
                return@launch
            }
            try {
                val me = network.authApi.me()
                // В твоей модели User id — строка (ты создаёшь User("u1", ...))
                user = User(
                    id = me.id.toString(),
                    phone = me.phone
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось загрузить профиль")
            }
        }
    }

    fun authRequestOtp(
        phone: String,
        onSuccess: (challengeId: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = authRepo.requestOtp(phone)
                onSuccess(res.challengeId)
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка запроса кода")
            }
        }
    }

    fun authVerifyOtp(
        challengeId: Long,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                authRepo.verifyOtp(challengeId, code)

                // токен получен — запрашиваем /me
                val me = network.authApi.me()
                user = User(
                    id = me.id.toString(),
                    phone = me.phone
                )

                loadClubs(force = true)
                loadShopData(force = true)
                syncCartProducts(force = true)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Неверный код или ошибка сети")
            }
        }
    }

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

    // --- Авторизация ---
    var user: User? by mutableStateOf(null)
        private set

    var clubs: List<Club> by mutableStateOf(emptyList())
        private set

    var clubsLoading: Boolean by mutableStateOf(false)
        private set

    var clubsError: String? by mutableStateOf(null)
        private set

    // --- Shop (server) ---
    var shopCategories: List<ProductCategory> by mutableStateOf(emptyList())
        private set

    var shopProducts: List<Product> by mutableStateOf(emptyList())
        private set

    var shopLoading: Boolean by mutableStateOf(false)
        private set

    var shopError: String? by mutableStateOf(null)
        private set

    // --- Cart (server, products only for now) ---
    var cartSyncLoading: Boolean by mutableStateOf(false)
        private set

    var cartSyncError: String? by mutableStateOf(null)
        private set

    /** История оплат (мок, но структура как под БД). */
    val purchaseHistory = mutableStateListOf<Purchase>()

    var historyLoading: Boolean by mutableStateOf(false)
        private set

    var historyError: String? by mutableStateOf(null)
        private set

    // --- Выбор клуба ---
    var selectedClubId: String by mutableStateOf(FakeData.clubs.first().id)
        private set

    var clubConfirmed: Boolean by mutableStateOf(false)
        private set

    // черновик бронирования (до добавления в корзину)
    var bookingDraft: BookingDraft by mutableStateOf(BookingDraft(clubId = selectedClubId))
        private set

    // --- Seats (server) ---
    /** Места выбранного клуба, загруженные с сервера. */
    var clubSeats: List<Seat> by mutableStateOf(emptyList())
        private set

    /** Занятые места в выбранном интервале (доступность с сервера). */
    var busySeatIds: Set<String> by mutableStateOf(emptySet())
        private set

    /** Позиции мест из опубликованного floorplan (0..1). Если схемы нет — список пустой. */
    var floorplanSeats: List<FloorplanSeatPos> by mutableStateOf(emptyList())
        private set

    var seatsLoading: Boolean by mutableStateOf(false)
        private set

    var seatsError: String? by mutableStateOf(null)
        private set

    /**
     * Строки брони в корзине ТЕКУЩЕГО клуба.
     * Для другого клуба см. [bookingCartLinesFor].
     */
    val bookingCartLines: SnapshotStateList<CartBookingLine>
        get() = cartFor(selectedClubId).bookingLines

    fun bookingCartLinesFor(clubId: String): SnapshotStateList<CartBookingLine> = cartFor(clubId).bookingLines

    // если пользователь редактирует бронь из корзины — тут хранится id строки
    var editingBookingId: String? by mutableStateOf(null)
        private set

    // чтобы при переходах по нижней панели "Бронь" возвращалась туда, где пользователь был в последний раз
    var lastBookingRoute: String by mutableStateOf(Routes.Booking)
        private set

    /** Товары/услуги в корзине ТЕКУЩЕГО клуба. */
    val cartLines: SnapshotStateList<CartProductLine>
        get() = cartFor(selectedClubId).productLines

    fun cartLinesFor(clubId: String): SnapshotStateList<CartProductLine> = cartFor(clubId).productLines

    // мок текущей сессии
    var currentSession: CurrentSessionSummary? by mutableStateOf(null)
        private set

    fun isLoggedIn(): Boolean = user != null

    fun setClub(clubId: String) {
        selectedClubId = clubId
        bookingDraft = BookingDraft(clubId = clubId)
        clubConfirmed = false
        editingBookingId = null

        // reset seat cache when switching clubs
        clubSeats = emptyList()
        busySeatIds = emptySet()
        seatsError = null
    }

    fun confirmClub() { clubConfirmed = true }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepo.logout()
            } catch (_: Exception) {
                // даже если сервер не ответил — локально всё равно выходим
            } finally {
                user = null
                shopCategories = emptyList()
                shopProducts = emptyList()
                purchaseHistory.clear()
                clearAllCarts()
                // перезагружаем клубы через публичный /clubs без blocked-статусов,
                // чтобы не показывать "Заблокирован" после выхода
                loadClubs(force = true)
            }
        }
    }

    private fun applyServerCartProducts(cart: CartResponseDto) {
        val lines = cartFor(selectedClubId).productLines
        lines.clear()
        cart.products.forEach { p ->
            lines.add(
                CartProductLine(
                    productId = p.productId.toString(),
                    title = p.title,
                    price = p.priceRub,
                    variant = null,
                    qty = p.qty,
                    lineId = p.lineId
                )
            )
        }
    }

    private fun applyServerCartBookings(cart: CartResponseDto) {
        val lines = cartFor(selectedClubId).bookingLines
        lines.clear()

        cart.bookings.forEach { b ->
            val start = LocalDateTime.parse(b.startAt)
            val end = LocalDateTime.parse(b.endAt)
            val baseDate = start.toLocalDate()
            val endOffsetDays = ChronoUnit.DAYS.between(baseDate, end.toLocalDate()).toInt().coerceAtLeast(0)

            lines.add(
                CartBookingLine(
                    id = b.lineId.toString(),
                    clubId = selectedClubId,
                    date = baseDate,
                    startDayOffset = 0,
                    startMin = start.hour * 60 + start.minute,
                    endDayOffset = endOffsetDays,
                    endMin = end.hour * 60 + end.minute,
                    packageHours = b.packageHours,
                    seatIds = b.seatIds.map { it.toString() }
                )
            )
        }
    }

    /** Загружает категории и меню клуба с сервера. */
    fun loadShopData(force: Boolean = false) {
        val clubIdLong = selectedClubIdLongOrNull() ?: return
        if (!force && shopCategories.isNotEmpty() && shopProducts.isNotEmpty()) return
        if (shopLoading) return

        shopLoading = true
        shopError = null

        viewModelScope.launch {
            try {
                val cats = catalogRepo.categories()
                    .sortedBy { it.sortOrder }
                    .map { ProductCategory(it.id.toString(), it.title) }

                val menu = catalogRepo.clubProducts(clubIdLong)
                    .filter { it.isAvailable }
                    .map {
                        Product(
                            id = it.productId.toString(),
                            categoryId = it.categoryId.toString(),
                            title = it.title,
                            price = it.priceRub,
                            description = it.description ?: "",
                            variants = emptyList()
                        )
                    }

                shopCategories = cats
                shopProducts = menu
            } catch (e: Exception) {
                shopError = e.message ?: "Не удалось загрузить меню"
            } finally {
                shopLoading = false
            }
        }
    }

    /** Синхронизирует товары серверной корзины в локальный cartLines. */
    fun syncCartProducts(force: Boolean = false) {
        val clubIdLong = selectedClubIdLongOrNull() ?: return
        if (user == null) return
        if (cartSyncLoading) return
        if (!force && cartLines.isNotEmpty() && bookingCartLines.isNotEmpty()) return

        cartSyncLoading = true
        cartSyncError = null

        viewModelScope.launch {
            try {
                val cart = cartRepo.getOrCreate(clubIdLong)
                applyServerCartProducts(cart)
                applyServerCartBookings(cart)
            } catch (e: HttpException) {
                cartSyncError = "Ошибка корзины: ${e.code()}"
            } catch (e: Exception) {
                cartSyncError = e.message ?: "Ошибка корзины"
            } finally {
                cartSyncLoading = false
            }
        }
    }

    /** Загружает места клуба и рассчитывает доступность для текущего интервала брони. */
    fun loadSeatsAndAvailability(force: Boolean = false) {
        val clubIdLong = selectedClubIdLongOrNull() ?: return

        // если изменилось только время — перезагрузку мест пропускаем, обновляем только доступность
        val shouldLoadSeats = force || clubSeats.isEmpty()

        // игнорируем повторный вызов пока идёт запрос
        if (seatsLoading) return

        seatsLoading = true
        seatsError = null

        val startAt = startDateTime(bookingDraft)
        val endAt = endDateTime(bookingDraft)

        viewModelScope.launch {
            try {
                if (shouldLoadSeats) {
                    val dtos = seatRepo.seats(clubIdLong)
                    clubSeats = dtos.map { dto ->
                        val type = runCatching { SeatType.valueOf(dto.type) }.getOrDefault(SeatType.REGULAR)
                        Seat(
                            id = dto.id.toString(),
                            label = dto.label,
                            type = type,
                            hasPc = true,
                            equipment = if (type == SeatType.VIP) "VIP" else "STANDARD",
                            booked = emptyList()
                        )
                    }
                }

                // загружаем floorplan + availability одним запросом
                // если схемы нет — fallback на availability
                try {
                    val fp = floorplanRepo.publishedWithAvailability(
                        clubId = clubIdLong,
                        from = startAt.toString(),
                        to = endAt.toString()
                    )

                    floorplanSeats = parseFloorplanSeatPositions(
                        data = fp.floorplan.data,
                        planW = fp.floorplan.width,
                        planH = fp.floorplan.height,
                        gridSize = fp.floorplan.gridSize
                    )

                    // busySeatIds может прийти и как busySeatIds, и как seats[].isBusy
                    val busy = if (fp.busySeatIds.isNotEmpty()) {
                        fp.busySeatIds
                    } else {
                        fp.seats.filter { it.isBusy }.map { it.seatId }
                    }
                    busySeatIds = busy.map { it.toString() }.toSet()
                } catch (e: HttpException) {
                    if (e.code() == 404) {
                        // схемы нет — просто availability
                        floorplanSeats = emptyList()
                        val availability = seatRepo.availability(clubIdLong, startAt.toString(), endAt.toString())
                        busySeatIds = availability
                            .filter { !it.isAvailable }
                            .map { it.seatId.toString() }
                            .toSet()
                    } else {
                        throw e
                    }
                }
            } catch (e: Exception) {
                seatsError = e.message ?: "Не удалось загрузить места"
            } finally {
                seatsLoading = false
            }
        }
    }

    private fun parseFloorplanSeatPositions(
        data: JsonElement,
        planW: Int,
        planH: Int,
        gridSize: Int
    ): List<FloorplanSeatPos> {
        if (planW <= 0 || planH <= 0) return emptyList()

        val root = runCatching { data.jsonObject }.getOrNull() ?: return emptyList()
        val items = root["items"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return emptyList()

        fun JsonObject.num(vararg keys: String): Double? {
            for (k in keys) {
                val el = this[k] ?: continue
                val prim = runCatching { el.jsonPrimitive }.getOrNull() ?: continue
                val v = prim.doubleOrNull
                if (v != null) return v
                // иногда числа прилетают строкой
                val s = prim.contentOrNull
                val v2 = s?.toDoubleOrNull()
                if (v2 != null) return v2
            }
            return null
        }

        fun JsonObject.str(key: String): String? =
            runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

        val out = mutableListOf<FloorplanSeatPos>()

        for (it in items) {
            val obj = runCatching { it.jsonObject }.getOrNull() ?: continue
            val type = obj.str("type") ?: continue
            if (type != "SEAT") continue

            val seatId = obj.num("seatId")?.toLong() ?: continue

            val x = obj.num("x", "left") ?: 0.0
            val y = obj.num("y", "top") ?: 0.0
            val w = obj.num("w", "width", "size") ?: gridSize.toDouble().coerceAtLeast(1.0)
            val h = obj.num("h", "height") ?: w

            out += FloorplanSeatPos(
                seatId = seatId.toString(),
                x = (x / planW.toDouble()).toFloat().coerceIn(0f, 1f),
                y = (y / planH.toDouble()).toFloat().coerceIn(0f, 1f),
                w = (w / planW.toDouble()).toFloat().coerceIn(0.01f, 1f),
                h = (h / planH.toDouble()).toFloat().coerceIn(0.01f, 1f)
            )
        }

        return out.sortedBy { it.seatId.toLongOrNull() ?: Long.MAX_VALUE }
    }

    fun addProduct(product: Product, variant: String?) {
        val clubIdLong = selectedClubIdLongOrNull()
        val productIdLong = product.id.toLongOrNull()

        // сервер пока поддерживает только товары без вариантов
        if (user != null && clubIdLong != null && productIdLong != null && variant == null) {
            viewModelScope.launch {
                try {
                    val cart = cartRepo.addProduct(clubIdLong, productIdLong, 1)
                    applyServerCartProducts(cart)
                } catch (_: Exception) {
                    // при ошибке — обновляем локально
                    val lines = cartFor(selectedClubId).productLines
                    val idx = lines.indexOfFirst { it.productId == product.id && it.variant == variant }
                    if (idx < 0) lines.add(CartProductLine(product.id, product.title, product.price, variant, 1))
                    else lines[idx] = lines[idx].copy(qty = lines[idx].qty + 1)
                }
            }
            return
        }

        // локально/мок
        val lines = cartFor(selectedClubId).productLines
        val idx = lines.indexOfFirst { it.productId == product.id && it.variant == variant }
        if (idx < 0) lines.add(CartProductLine(product.id, product.title, product.price, variant, 1))
        else lines[idx] = lines[idx].copy(qty = lines[idx].qty + 1)
    }

    fun changeQty(productId: String, variant: String?, delta: Int) {
        val lines = cartFor(selectedClubId).productLines
        val idx = lines.indexOfFirst { it.productId == productId && it.variant == variant }
        if (idx < 0) return

        val cur = lines[idx]
        val newQty = (cur.qty + delta).coerceAtLeast(0)

        val clubIdLong = selectedClubIdLongOrNull()
        val lineId = cur.lineId

        // серверное обновление (только для товаров без вариантов)
        if (user != null && clubIdLong != null && lineId != null && variant == null) {
            viewModelScope.launch {
                try {
                    val cart = if (newQty <= 0) {
                        cartRepo.deleteProductLine(clubIdLong, lineId)
                    } else {
                        cartRepo.updateQty(clubIdLong, lineId, newQty)
                    }
                    applyServerCartProducts(cart)
                } catch (_: Exception) {
                    // при ошибке — обновляем локально
                    if (newQty == 0) lines.removeAt(idx) else lines[idx] = cur.copy(qty = newQty)
                }
            }
            return
        }

        // локально/мок
        if (newQty == 0) lines.removeAt(idx) else lines[idx] = cur.copy(qty = newQty)
    }

    /** Очистить корзину ОДНОГО клуба (по умолчанию — текущего). */
    fun clearCart(clubId: String = selectedClubId) {
        val clubIdLong = (clubId.toLongOrNull() ?: clubId.removePrefix("c").toLongOrNull())
        if (user != null && clubIdLong != null) {
            viewModelScope.launch {
                try {
                    cartRepo.clear(clubIdLong)
                } catch (_: Exception) {
                } finally {
                    cartFor(clubId).productLines.clear()
                    cartFor(clubId).bookingLines.clear()
                    if (clubId == selectedClubId) {
                        bookingDraft = BookingDraft(clubId = selectedClubId)
                        editingBookingId = null
                    }
                }
            }
            return
        }

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
        // ищем строку во всех корзинах: id уникальны, но клуб может быть не текущим
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
    fun addCurrentBookingToCartAsync(onResult: (CommitResult) -> Unit) = commitCurrentBookingToCartAsync(onResult)

    /**
     * Коммит брони в корзину:
     * - если editingBookingId != null => обновляем строку
     * - иначе добавляем новую
     * - не допускаем повторов: один и тот же компьютер не может быть забронирован
     *   пользователем в пересекающийся интервал (даже повторно “самому себе”).
     */
    fun commitCurrentBookingToCartAsync(onResult: (CommitResult) -> Unit) {
        val d = bookingDraft
        if (d.selectedSeatIds.isEmpty()) {
            onResult(CommitResult(false, "Выбери места"))
            return
        }

        val start = startDateTime(d)
        val end = endDateTime(d)
        if (!start.isBefore(end)) {
            onResult(CommitResult(false, "Некорректное время"))
            return
        }

        val clubIdLong = selectedClubIdLongOrNull()
        val seatIdsLong = d.selectedSeatIds.mapNotNull { it.toLongOrNull() }
        if (user == null || clubIdLong == null || seatIdsLong.size != d.selectedSeatIds.size) {
            // пользователь не авторизован или id нечисловой — добавляем локально
            onResult(commitCurrentBookingToCartLocal())
            return
        }

        viewModelScope.launch {
            try {
                val before = cartRepo.getOrCreate(clubIdLong)
                val beforeIds = before.bookings.map { it.lineId }.toSet()

                // при редактировании сначала удаляем старую строку — бэкенд не поддерживает PATCH для интервала
                val editingIdLong = editingBookingId?.toLongOrNull()
                if (editingIdLong != null) {
                    runCatching { cartRepo.deleteBookingLine(clubIdLong, editingIdLong) }
                }

                val afterAdd = cartRepo.addBooking(
                    clubId = clubIdLong,
                    startAtIso = start.toString(),
                    endAtIso = end.toString(),
                    packageHours = d.packageHours
                )

                val newLineId = afterAdd.bookings
                    .firstOrNull { it.lineId !in beforeIds }
                    ?.lineId
                    ?: afterAdd.bookings.maxByOrNull { it.lineId }?.lineId
                    ?: throw IllegalStateException("Не удалось определить lineId брони")

                val afterSeats = cartRepo.setBookingSeats(clubIdLong, newLineId, seatIdsLong)

                applyServerCartProducts(afterSeats)
                applyServerCartBookings(afterSeats)

                editingBookingId = null
                bookingDraft = d.copy(selectedSeatIds = emptySet())
                onResult(CommitResult(true))
            } catch (e: HttpException) {
                onResult(CommitResult(false, "Ошибка брони: ${e.code()}"))
            } catch (e: Exception) {
                onResult(CommitResult(false, e.message ?: "Не удалось добавить бронь"))
            }
        }
    }

    /** Локальная/мок корзина брони — используется когда пользователь не авторизован. */
    private fun commitCurrentBookingToCartLocal(): CommitResult {
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
        val clubIdLong = selectedClubIdLongOrNull()
        val lineIdLong = id.toLongOrNull()

        if (user != null && clubIdLong != null && lineIdLong != null) {
            viewModelScope.launch {
                try {
                    val cart = cartRepo.deleteBookingLine(clubIdLong, lineIdLong)
                    applyServerCartProducts(cart)
                    applyServerCartBookings(cart)
                } catch (_: Exception) {
                    // при ошибке — обновляем локально
                    cartsByClub.values.forEach { it.bookingLines.removeAll { line -> line.id == id } }
                } finally {
                    if (editingBookingId == id) editingBookingId = null
                }
            }
            return
        }

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
        // ручной выбор конца: пакет сохраняем только если конец строго соответствует пакету
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
        if (clubSeats.isEmpty()) return false
        val free = clubSeats
            .filter { it.hasPc }
            .firstOrNull { it.id !in busySeatIds }
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
            // для wrap-диапазонов оставляем PARTIAL
            val fullyCovered = seat.booked.any { it.startMin <= selected.startMin && selected.endMin <= it.endMin }
            return if (fullyCovered) SeatAvailability.BOOKED else SeatAvailability.PARTIAL
        }

        // 2) Риск продления: считаем, что любая бронь может продлиться до +2 часов
        // => если выбранный диапазон попадает в расширенный интервал — место блокируем
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

    /**
     * Мгновенное оформление (без внутреннего кошелька).
     *
     * Создаём Purchase (как под БД) и кладём в [purchaseHistory],
     * затем очищаем корзину текущего клуба.
     */
    fun checkoutServer(
        onSuccess: (purchaseId: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val clubIdLong = selectedClubIdLongOrNull()
        if (user == null || clubIdLong == null) {
            onError("Сначала войдите и выберите клуб")
            return
        }

        if (cartLines.isEmpty() && bookingCartLines.isEmpty()) {
            onError("Корзина пуста")
            return
        }

        viewModelScope.launch {
            try {
                val resp = checkoutRepo.checkout(UUID.randomUUID().toString(), clubIdLong)
                // сервер после успешного checkout очищает корзину
                cartFor(selectedClubId).productLines.clear()
                cartFor(selectedClubId).bookingLines.clear()
                // Немедленная оплата (CREATED → PAID)
                try { checkoutRepo.payPurchase(resp.purchaseId) } catch (_: Exception) { }
                loadPurchaseHistory()
                onSuccess(resp.purchaseId)
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка оплаты")
            }
        }
    }

    fun loadPurchaseHistory() {
        if (user == null) return
        viewModelScope.launch {
            historyLoading = true
            historyError = null
            try {
                val summaries = checkoutRepo.getMyPurchases()
                val detailed = summaries.map { checkoutRepo.getPurchaseDetails(it.purchaseId) }
                purchaseHistory.clear()
                purchaseHistory.addAll(detailed.map { it.toPurchase(clubs) })
            } catch (e: Exception) {
                historyError = e.message ?: "Ошибка загрузки истории"
            } finally {
                historyLoading = false
            }
        }
    }

    fun payPurchase(purchaseId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                checkoutRepo.payPurchase(purchaseId)
                loadPurchaseHistory()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка оплаты")
            }
        }
    }

    fun cancelPurchase(purchaseId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                checkoutRepo.cancelPurchase(purchaseId)
                loadPurchaseHistory()
                // сразу сбрасываем локальный кэш занятости и корзины — иначе экран мест
                // покажет место как занятое (launchSingleTop не перезапускает LaunchedEffect)
                busySeatIds = emptySet()
                cartFor(selectedClubId).bookingLines.clear()
                // затем обновляем с сервера (если клуб выбран)
                loadSeatsAndAvailability(force = true)
                syncCartProducts(force = true)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка отмены")
            }
        }
    }

    private fun PurchaseDetailsDto.toPurchase(clubs: List<Club>): Purchase {
        val clubName = clubs.firstOrNull { it.id == clubId.toString() }?.name ?: "Клуб"
        val createdAtDt = LocalDateTime.parse(createdAt)

        val bookingOrders = bookingItems.map { b ->
            BookingOrder(
                id = b.bookingId.toString(),
                clubId = this.clubId.toString(),
                clubName = clubName,
                startAt = LocalDateTime.parse(b.startAt),
                endAt = LocalDateTime.parse(b.endAt),
                seatIds = b.seatIds.map { it.toString() },
                seatLabels = b.seatLabels,
                packageHours = null,
                rateRubPerHour = 200,
                totalRub = b.totalRub
            )
        }

        val productOrder: ProductOrder? = if (productItems.isEmpty()) null else {
            ProductOrder(
                id = "po-$purchaseId",
                clubId = this.clubId.toString(),
                clubName = clubName,
                createdAt = createdAtDt,
                readyBy = null,
                readyByPolicy = ReadyByPolicy.ASAP,
                status = ProductOrderStatus.NOT_READY,
                items = productItems.map { i ->
                    ProductOrderItemSnapshot(
                        productId = i.productId.toString(),
                        title = i.name,
                        variant = null,
                        priceRub = i.unitRub,
                        qty = i.qty
                    )
                },
                totalRub = productsTotalRub
            )
        }

        return Purchase(
            id = purchaseId.toString(),
            clubId = this.clubId.toString(),
            clubName = clubName,
            createdAt = createdAtDt,
            bookingOrders = bookingOrders,
            productOrder = productOrder,
            bookingTotalRub = bookingTotalRub,
            productsTotalRub = productsTotalRub,
            totalRub = totalRub,
            paymentStatus = paymentStatus
        )
    }

    fun checkoutInstant(): Boolean {
        val clubId = selectedClubId
        val cart = cartFor(clubId)
        if (cart.bookingLines.isEmpty() && cart.productLines.isEmpty()) return false

        val purchase = buildPurchaseFromCart(clubId)
        purchaseHistory.add(0, purchase)

        // мок: запускаем “текущую сессию” по последней броне
        mockStartSession(cart.bookingLines.lastOrNull())

        clearCart(clubId)
        return true
    }

    private fun buildPurchaseFromCart(clubId: String): Purchase {
        val cart = cartFor(clubId)
        val clubName = FakeData.clubs.firstOrNull { it.id == clubId }?.name ?: "Клуб"

        // дата брони — моковая (см. BookingDraft.date), поэтому "текущее" время делаем на той же шкале
        // иначе история выглядит "в прошлом" из-за реального времени телефона
        val mockNow = bookingDraft.date.atStartOfDay().plusMinutes(bookingDraft.startMin.toLong())

        val bookingOrders = cart.bookingLines.mapIndexed { idx, line ->
            val startAt = lineStartDateTime(line)
            val endAt = lineEndDateTime(line)
            val seats = FakeData.seatMapByClub[line.clubId].orEmpty()
                .filter { s -> line.seatIds.contains(s.id) }
            val seatLabels = seats.map { it.label }
            val rate = bookingRateRubPerHour(line.packageHours)
            val cost = bookingLineCost(line)

            BookingOrder(
                id = "bo-${System.currentTimeMillis()}-$idx",
                clubId = line.clubId,
                clubName = clubName,
                startAt = startAt,
                endAt = endAt,
                seatIds = line.seatIds,
                seatLabels = if (seatLabels.isEmpty()) line.seatIds else seatLabels,
                packageHours = line.packageHours,
                rateRubPerHour = rate,
                totalRub = cost,
                status = BookingStatus.UPCOMING
            )
        }

        val bookingTotal = bookingOrders.sumOf { it.totalRub }

        val productsTotal = cart.productLines.sumOf { it.price * it.qty }

        val productOrder: ProductOrder? = if (cart.productLines.isEmpty()) {
            null
        } else {
            val readyByResult = calcReadyBy(mockNow, bookingOrders)
            val items = cart.productLines.map {
                ProductOrderItemSnapshot(
                    productId = it.productId,
                    title = it.title,
                    variant = it.variant,
                    priceRub = it.price,
                    qty = it.qty
                )
            }

            ProductOrder(
                id = "po-${System.currentTimeMillis()}",
                clubId = clubId,
                clubName = clubName,
                createdAt = mockNow,
                readyBy = readyByResult.readyBy,
                readyByPolicy = readyByResult.policy,
                status = ProductOrderStatus.NOT_READY,
                items = items,
                totalRub = productsTotal
            )
        }

        return Purchase(
            id = "p-${System.currentTimeMillis()}",
            clubId = clubId,
            clubName = clubName,
            createdAt = mockNow,
            bookingOrders = bookingOrders,
            productOrder = productOrder,
            bookingTotalRub = bookingTotal,
            productsTotalRub = productsTotal,
            totalRub = bookingTotal + productsTotal
        )
    }

    private data class ReadyByResult(val readyBy: LocalDateTime, val policy: ReadyByPolicy)

    /**
     * AUTO-правило времени готовности, чтобы уже сейчас история была понятной:
     * - если ближайшая бронь стартует в течение 30 минут — готовность к началу брони
     * - иначе — как можно скорее (мок: +15 минут)
     */
    private fun calcReadyBy(now: LocalDateTime, bookings: List<BookingOrder>): ReadyByResult {
        val soonestStart = bookings.minByOrNull { it.startAt }?.startAt
        if (soonestStart != null) {
            val minutes = Duration.between(now, soonestStart).toMinutes()
            if (minutes in 0..30) return ReadyByResult(soonestStart, ReadyByPolicy.BOOKING_START)
        }
        return ReadyByResult(now.plusMinutes(15), ReadyByPolicy.ASAP)
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

        if (user != null) {
            loadShopData(force = true)
            syncCartProducts(force = true)
        }
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

        // пакет НЕ сбрасываем если новый конец равен (начало + пакет)
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

    fun loadClubs(force: Boolean = false) {
        if (clubsLoading) return
        if (!force && clubs.isNotEmpty()) return

        clubsLoading = true
        clubsError = null

        viewModelScope.launch {
            try {
                val loaded: List<Club> = if (user != null) {
                    // пробуем /available (с флагом isBlocked)
                    try {
                        clubsRepo.getAvailable().map { dto ->
                            Club(
                                id = dto.id.toString(),
                                name = dto.name,
                                location = dto.locationText ?: "",
                                address = dto.address,
                                description = dto.description ?: "",
                                isBlocked = dto.isBlocked,
                                blockReason = dto.blockReason
                            )
                        }
                    } catch (_: Exception) {
                        // если не получилось (например 401) — fallback на /clubs
                        clubsRepo.getClubs().map { dto ->
                            Club(
                                id = dto.id.toString(),
                                name = dto.name,
                                location = dto.locationText ?: "",
                                address = dto.address,
                                description = dto.description ?: ""
                            )
                        }
                    }
                } else {
                    clubsRepo.getClubs().map { dto ->
                        Club(
                            id = dto.id.toString(),
                            name = dto.name,
                            location = dto.locationText ?: "",
                            address = dto.address,
                            description = dto.description ?: ""
                        )
                    }
                }

                clubs = loaded
                ensureSelectedClubValid()
            } catch (e: Exception) {
                clubsError = e.message ?: "Не удалось загрузить клубы"
            } finally {
                clubsLoading = false
            }
        }
    }

    private fun ensureSelectedClubValid() {
        if (clubs.isEmpty()) return
        val ids = clubs.map { it.id }.toSet()
        if (selectedClubId !in ids) {
            setClub(clubs.first().id)
        }
    }
}
