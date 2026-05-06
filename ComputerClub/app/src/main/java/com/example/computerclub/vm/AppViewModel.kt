package com.example.computerclub.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.computerclub.model.*
import com.example.computerclub.app.Routes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalTime
import kotlin.math.ceil
import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.computerclub.data.local.TokenStore
import com.example.computerclub.data.network.NetworkClient
import com.example.computerclub.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.computerclub.data.repository.ClubsRepository
import com.example.computerclub.data.repository.CatalogRepository
import com.example.computerclub.data.repository.CartRepository
import com.example.computerclub.data.repository.CheckoutRepository
import com.example.computerclub.data.repository.SeatRepository
import com.example.computerclub.data.repository.FloorplanRepository
import com.example.computerclub.data.repository.FavoritesRepository
import com.example.computerclub.data.repository.ReportRepository
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



class AppViewModel(app: Application) : AndroidViewModel(app) {

    // Не зависим от BuildConfig (часто ломается при изменении namespace/applicationId)
    private val isDebuggable: Boolean =
        (getApplication<Application>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val baseUrl = "http://10.0.2.2:8080" // для Android Emulator; поменяй на свой хост

    private val network = NetworkClient(
        context = getApplication(),
        baseUrl = baseUrl,
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
    private val favoritesRepo = FavoritesRepository(network.favoritesApi)
    private val timePackageApi = network.timePackageApi
    private val seatPriceApi = network.seatPriceApi
    private val reportRepo = ReportRepository(network.reportApi)

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
                user = User(
                    id = me.id.toString(),
                    phone = me.phone
                )
                loadFavorites()
                onSuccess()
            } catch (e: Exception) {
                onError("Не удалось загрузить профиль")
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
                onError("Ошибка запроса кода")
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
                loadTimePackages(force = true)
                syncCartProducts(force = true)
                loadFavorites()
                onSuccess()
            } catch (e: Exception) {
                onError("Неверный код или ошибка сети")
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

    // --- Пакеты времени ---
    var timePackages: List<com.example.computerclub.data.network.dto.TimePackageResponseDto> by mutableStateOf(emptyList())
        private set

    // --- Цены за места ---
    var seatPrices: List<com.example.computerclub.data.network.dto.SeatPriceResponseDto> by mutableStateOf(emptyList())
        private set

    // --- Характеристики мест (загружаются вместе с местами) ---
    var seatSpecs: List<com.example.computerclub.data.network.dto.SeatSpecResponseDto> by mutableStateOf(emptyList())
        private set

    /** Минимальная цена за час по всем типам мест — стандартный тариф. */
    val standardRateRubPerHour: Int?
        get() = seatPrices.minOfOrNull { it.pricePerHourRub }

    // --- Shop (server) ---
    var shopCategories: List<ProductCategory> by mutableStateOf(emptyList())
        private set

    var shopProducts: List<Product> by mutableStateOf(emptyList())
        private set

    var shopLoading: Boolean by mutableStateOf(false)
        private set

    var shopError: String? by mutableStateOf(null)
        private set

    // --- Детали клуба (просмотр до выбора) ---
    var clubDetailsSpecs: List<com.example.computerclub.data.network.dto.SeatSpecResponseDto> by mutableStateOf(emptyList())
        private set

    var clubDetailsPrices: List<com.example.computerclub.data.network.dto.SeatPriceResponseDto> by mutableStateOf(emptyList())
        private set

    var clubDetailsPackages: List<com.example.computerclub.data.network.dto.TimePackageResponseDto> by mutableStateOf(emptyList())
        private set

    var clubDetailsProducts: List<com.example.computerclub.data.network.dto.ClubProductResponseDto> by mutableStateOf(emptyList())
        private set

    var clubDetailsLoading: Boolean by mutableStateOf(false)
        private set

    // id клуба, для которого загружены детали — чтобы не перезагружать при повторном открытии
    private var clubDetailsLoadedForId: Long? = null

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
    var selectedClubId: String by mutableStateOf("")
        private set

    // флаг: для какого клуба уже выполнена первичная синхронизация корзины
    private var cartSyncedClubId: String? = null

    // активная джоба синхронизации — отменяется при любой мутации корзины
    private var cartSyncJob: Job? = null

    // true после любой мутации корзины (добавление/удаление/изменение qty);
    // предотвращает перезапись локальных изменений автоматической синхронизацией
    private var localCartModified = false

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

    /** Стены между ячейками из опубликованного floorplan. */
    var floorplanWalls: List<FloorplanWallPos> by mutableStateOf(emptyList())
        private set

    /** Ячейки пола (комнаты) из опубликованного floorplan. */
    var floorplanFloor: List<FloorplanFloorPos> by mutableStateOf(emptyList())
        private set

    /** Размеры сетки опубликованного floorplan (0 если схемы нет). */
    var floorplanNumCols: Int by mutableStateOf(0)
        private set
    var floorplanNumRows: Int by mutableStateOf(0)
        private set

    var seatsLoading: Boolean by mutableStateOf(false)
        private set

    var seatsError: String? by mutableStateOf(null)
        private set

    /** Строки брони в корзине ТЕКУЩЕГО клуба. */
    val bookingCartLines: SnapshotStateList<CartBookingLine>
        get() = cartFor(selectedClubId).bookingLines

    // если пользователь редактирует бронь из корзины — тут хранится id строки
    var editingBookingId: String? by mutableStateOf(null)
        private set

    // чтобы при переходах по нижней панели "Бронь" возвращалась туда, где пользователь был в последний раз
    var lastBookingRoute: String by mutableStateOf(Routes.Booking)
        private set

    /** Товары/услуги в корзине ТЕКУЩЕГО клуба. */
    val cartLines: SnapshotStateList<CartProductLine>
        get() = cartFor(selectedClubId).productLines

    fun isLoggedIn(): Boolean = user != null

    fun setClub(clubId: String) {
        selectedClubId = clubId
        bookingDraft = BookingDraft(clubId = clubId)
        clubConfirmed = false
        editingBookingId = null

        clubSeats = emptyList()
        busySeatIds = emptySet()
        seatsError = null
        // сбрасываем схему зала — иначе при смене клуба показывается старая схема до загрузки новой
        floorplanSeats = emptyList()
        floorplanWalls = emptyList()
        floorplanFloor = emptyList()
        floorplanNumCols = 0
        floorplanNumRows = 0
        cartSyncedClubId = null
        localCartModified = false
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
                favoriteClubIds.clear()
                cartSyncedClubId = null
                localCartModified = false
                // перезагружаем клубы через публичный /clubs без blocked-статусов,
                // чтобы не показывать "Заблокирован" после выхода
                loadClubs(force = true)
            }
        }
    }

    private fun applyServerCartProducts(cart: CartResponseDto, clubId: String) {
        val lines = cartFor(clubId).productLines
        Log.d("Cart", "applyServerCartProducts: replacing ${lines.size} items with ${cart.products.size} from server")
        lines.clear()
        cart.products.forEach { p ->
            lines.add(
                CartProductLine(
                    productId = p.productId.toString(),
                    title = p.title,
                    price = p.priceRub,
                    variant = null,
                    qty = p.qty,
                    lineId = p.lineId,
                    clubId = clubId
                )
            )
        }
    }

    private fun applyServerCartBookings(cart: CartResponseDto, clubId: String) {
        val lines = cartFor(clubId).bookingLines
        lines.clear()

        cart.bookings.forEach { b ->
            val start = LocalDateTime.parse(b.startAt)
            val end = LocalDateTime.parse(b.endAt)
            val baseDate = start.toLocalDate()
            val endOffsetDays = ChronoUnit.DAYS.between(baseDate, end.toLocalDate()).toInt().coerceAtLeast(0)

            lines.add(
                CartBookingLine(
                    id = b.lineId.toString(),
                    clubId = clubId,
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

    fun loadTimePackages(force: Boolean = false) {
        val clubIdLong = selectedClubIdLongOrNull() ?: return
        if (!force && timePackages.isNotEmpty()) return
        viewModelScope.launch {
            try {
                timePackages = timePackageApi.getPackages(clubIdLong)
            } catch (_: Exception) {
                // сохраняем предыдущие пакеты если сервер недоступен
            }
        }
    }

    fun loadSeatPrices(force: Boolean = false) {
        val clubIdLong = selectedClubIdLongOrNull() ?: return
        if (!force && seatPrices.isNotEmpty()) return
        viewModelScope.launch {
            try {
                seatPrices = seatPriceApi.getPrices(clubIdLong)
            } catch (_: Exception) {
                // не критично: стандартный пакет просто не отображается
            }
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
                shopError = "Не удалось загрузить меню"
            } finally {
                shopLoading = false
            }
        }
    }

    /** Загружает характеристики, цены, пакеты и товары для страницы деталей клуба.
     * Данные привязаны к [clubId] (клуб ещё не выбран), поэтому хранятся отдельно от
     * seatSpecs/seatPrices/timePackages, которые используются в бронировании.
     */
    fun loadClubDetailsExtras(clubId: Long, force: Boolean = false) {
        if (!force && clubDetailsLoadedForId == clubId) return
        if (clubDetailsLoading) return

        clubDetailsLoading = true
        viewModelScope.launch {
            try {
                val specsJob = launch {
                    clubDetailsSpecs = runCatching { seatRepo.seatSpecs(clubId) }.getOrDefault(emptyList())
                }
                val pricesJob = launch {
                    clubDetailsPrices = runCatching { seatPriceApi.getPrices(clubId) }.getOrDefault(emptyList())
                }
                val packagesJob = launch {
                    clubDetailsPackages = runCatching { timePackageApi.getPackages(clubId) }.getOrDefault(emptyList())
                }
                val productsJob = launch {
                    clubDetailsProducts = runCatching {
                        catalogRepo.clubProducts(clubId).filter { it.isAvailable }
                    }.getOrDefault(emptyList())
                }
                specsJob.join()
                pricesJob.join()
                packagesJob.join()
                productsJob.join()
                clubDetailsLoadedForId = clubId
            } finally {
                clubDetailsLoading = false
            }
        }
    }

    /** Синхронизирует корзину с сервером.
     * Auto-sync (force=false) пропускается если пользователь уже менял корзину локально —
     * это предотвращает перезапись оптимистичных удалений/добавлений данными с сервера.
     */
    fun syncCartProducts(force: Boolean = false) {
        val clubIdLong = selectedClubIdLongOrNull() ?: return
        if (user == null) return
        if (cartSyncLoading) return
        if (!force && cartSyncedClubId == selectedClubId) return
        // после любой локальной мутации auto-sync не трогает корзину
        if (!force && localCartModified) return

        // фиксируем клуб в момент запроса — selectedClubId может смениться до получения ответа
        val snapshotClubId = selectedClubId

        cartSyncLoading = true
        cartSyncError = null

        cartSyncJob = viewModelScope.launch {
            try {
                val cart = cartRepo.getOrCreate(clubIdLong)
                // при force-sync (логин, смена клуба, отмена покупки) сбрасываем флаг
                // и доверяем серверу; при auto-sync сюда не доходим (заблокировано выше)
                localCartModified = false
                applyServerCartProducts(cart, snapshotClubId)
                applyServerCartBookings(cart, snapshotClubId)
                cartSyncedClubId = snapshotClubId
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: HttpException) {
                cartSyncError = "Ошибка корзины: ${e.code()}"
            } catch (e: Exception) {
                cartSyncError = "Ошибка корзины"
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
                    seatSpecs = runCatching { seatRepo.seatSpecs(clubIdLong) }.getOrDefault(emptyList())
                }

                // загружаем floorplan + availability одним запросом
                // если схемы нет — fallback на availability
                try {
                    val fp = floorplanRepo.publishedWithAvailability(
                        clubId = clubIdLong,
                        from = startAt.toString(),
                        to = endAt.toString()
                    )

                    val nCols = fp.floorplan.width / fp.floorplan.gridSize
                    val nRows = fp.floorplan.height / fp.floorplan.gridSize
                    floorplanNumCols = nCols
                    floorplanNumRows = nRows
                    floorplanSeats = parseFloorplanSeatPositions(
                        data = fp.floorplan.data,
                        planW = fp.floorplan.width,
                        planH = fp.floorplan.height,
                        gridSize = fp.floorplan.gridSize
                    )
                    floorplanWalls = parseFloorplanWallPositions(
                        data = fp.floorplan.data,
                        numCols = nCols,
                        numRows = nRows
                    )
                    floorplanFloor = parseFloorplanFloorPositions(
                        data = fp.floorplan.data,
                        numCols = nCols,
                        numRows = nRows
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
                        floorplanWalls = emptyList()
                        floorplanFloor = emptyList()
                        floorplanNumCols = 0
                        floorplanNumRows = 0
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
                seatsError = "Не удалось загрузить места"
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
        if (planW <= 0 || planH <= 0 || gridSize <= 0) return emptyList()

        val root = runCatching { data.jsonObject }.getOrNull() ?: return emptyList()
        val items = root["items"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return emptyList()

        val numCols = planW / gridSize
        val numRows = planH / gridSize
        if (numCols <= 0 || numRows <= 0) return emptyList()

        fun JsonObject.num(vararg keys: String): Double? {
            for (k in keys) {
                val el = this[k] ?: continue
                val prim = runCatching { el.jsonPrimitive }.getOrNull() ?: continue
                val v = prim.doubleOrNull
                if (v != null) return v
                val v2 = prim.contentOrNull?.toDoubleOrNull()
                if (v2 != null) return v2
            }
            return null
        }

        fun JsonObject.str(key: String): String? =
            runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

        val out = mutableListOf<FloorplanSeatPos>()

        for (it in items) {
            val obj = runCatching { it.jsonObject }.getOrNull() ?: continue
            if (obj.str("type") != "SEAT") continue
            val seatId = obj.num("seatId")?.toLong() ?: continue

            // новый формат: col/row
            val col = obj.num("col")
            val row = obj.num("row")
            if (col != null && row != null) {
                out += FloorplanSeatPos(
                    seatId = seatId.toString(),
                    x = (col / numCols.toDouble()).toFloat().coerceIn(0f, 1f),
                    y = (row / numRows.toDouble()).toFloat().coerceIn(0f, 1f),
                    w = (1f / numCols).coerceIn(0.001f, 1f),
                    h = (1f / numRows).coerceIn(0.001f, 1f)
                )
                continue
            }

            // старый формат: x/y/w/h (абсолютные пиксели или дроби)
            val x = obj.num("x", "left") ?: 0.0
            val y = obj.num("y", "top") ?: 0.0
            val w = obj.num("w", "width", "size") ?: gridSize.toDouble().coerceAtLeast(1.0)
            val h = obj.num("h", "height") ?: w
            out += FloorplanSeatPos(
                seatId = seatId.toString(),
                x = (x / planW.toDouble()).toFloat().coerceIn(0f, 1f),
                y = (y / planH.toDouble()).toFloat().coerceIn(0f, 1f),
                w = (w / planW.toDouble()).toFloat().coerceIn(0.001f, 1f),
                h = (h / planH.toDouble()).toFloat().coerceIn(0.001f, 1f)
            )
        }

        return out.sortedBy { it.seatId.toLongOrNull() ?: Long.MAX_VALUE }
    }

    private fun parseFloorplanWallPositions(
        data: JsonElement,
        numCols: Int,
        numRows: Int
    ): List<FloorplanWallPos> {
        if (numCols <= 0 || numRows <= 0) return emptyList()

        val root = runCatching { data.jsonObject }.getOrNull() ?: return emptyList()
        val items = root["items"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return emptyList()

        fun JsonObject.num(key: String): Double? {
            val el = this[key] ?: return null
            val prim = runCatching { el.jsonPrimitive }.getOrNull() ?: return null
            return prim.doubleOrNull ?: prim.contentOrNull?.toDoubleOrNull()
        }

        fun JsonObject.str(key: String): String? =
            runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

        val out = mutableListOf<FloorplanWallPos>()

        for (it in items) {
            val obj = runCatching { it.jsonObject }.getOrNull() ?: continue
            if (obj.str("type") != "WALL") continue

            val orientation = when (obj.str("orientation")) {
                "H" -> WallOrientation.H
                "V" -> WallOrientation.V
                else -> continue
            }
            val col = obj.num("col")?.toInt() ?: continue
            val row = obj.num("row")?.toInt() ?: continue

            // H: граница между строками row-1 и row, col ∈ 0..numCols-1
            // V: граница между столбцами col-1 и col, row ∈ 0..numRows-1
            val valid = when (orientation) {
                WallOrientation.H -> col in 0 until numCols && row in 1 until numRows
                WallOrientation.V -> col in 1 until numCols && row in 0 until numRows
            }
            if (!valid) continue

            out += FloorplanWallPos(orientation = orientation, col = col, row = row)
        }

        return out
    }

    private fun parseFloorplanFloorPositions(
        data: JsonElement,
        numCols: Int,
        numRows: Int
    ): List<FloorplanFloorPos> {
        if (numCols <= 0 || numRows <= 0) return emptyList()

        val root = runCatching { data.jsonObject }.getOrNull() ?: return emptyList()
        val items = root["items"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return emptyList()

        fun JsonObject.num(key: String): Double? {
            val el = this[key] ?: return null
            val prim = runCatching { el.jsonPrimitive }.getOrNull() ?: return null
            return prim.doubleOrNull ?: prim.contentOrNull?.toDoubleOrNull()
        }

        fun JsonObject.str(key: String): String? =
            runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

        val out = mutableListOf<FloorplanFloorPos>()

        for (it in items) {
            val obj = runCatching { it.jsonObject }.getOrNull() ?: continue
            if (obj.str("type") != "FLOOR") continue
            val col = obj.num("col")?.toInt() ?: continue
            val row = obj.num("row")?.toInt() ?: continue
            val roomType = obj.str("roomType") ?: continue
            if (roomType != "REGULAR" && roomType != "VIP") continue
            if (col !in 0 until numCols || row !in 0 until numRows) continue
            out += FloorplanFloorPos(col = col, row = row, roomType = roomType)
        }

        return out
    }

    /** Берёт clubId из самой строки корзины; selectedClubId — только запасной вариант. */
    private fun resolveClubIdLongOrNull(line: CartProductLine): Long? =
        line.clubId?.toLongOrNull()
            ?: line.clubId?.removePrefix("c")?.toLongOrNull()
            ?: selectedClubIdLongOrNull()

    fun addProduct(product: Product, variant: String?) {
        val lines = cartFor(selectedClubId).productLines
        val idx = lines.indexOfFirst { it.productId == product.id && it.variant == variant }
        val prev = if (idx >= 0) lines[idx] else null

        // оптимистичное обновление — UI реагирует мгновенно
        if (idx < 0) lines.add(CartProductLine(product.id, product.title, product.price, variant, 1, clubId = selectedClubId))
        else lines[idx] = lines[idx].copy(qty = lines[idx].qty + 1)

        // после мутации auto-sync не должен перезаписывать локальное состояние
        localCartModified = true
        cartSyncJob?.cancel()
        cartSyncJob = null

        val clubIdLong = selectedClubIdLongOrNull()
        val productIdLong = product.id.toLongOrNull()

        if (user != null && clubIdLong != null && productIdLong != null && variant == null) {
            viewModelScope.launch {
                try {
                    val cart = cartRepo.addProduct(clubIdLong, productIdLong, 1)
                    val serverLine = cart.products.firstOrNull { it.productId == productIdLong }
                    if (serverLine != null) {
                        val curIdx = lines.indexOfFirst { it.productId == product.id && it.variant == variant }
                        if (curIdx < 0) {
                            // строку уже удалили локально пока летел запрос — убираем и на сервере
                            Log.d("Cart", "addProduct: line removed locally before response, deleting from server lineId=${serverLine.lineId}")
                            runCatching { cartRepo.deleteProductLine(clubIdLong, serverLine.lineId) }
                        } else {
                            val currentLine = lines[curIdx]
                            lines[curIdx] = currentLine.copy(lineId = serverLine.lineId)
                            // если qty уже изменилось пока запрос летел — синхронизируем
                            if (currentLine.qty != serverLine.qty) {
                                Log.d("Cart", "addProduct: qty drifted local=${currentLine.qty} server=${serverLine.qty}, syncing")
                                runCatching { cartRepo.updateQty(clubIdLong, serverLine.lineId, currentLine.qty) }
                            }
                        }
                    }
                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Cart", "addProduct failed: productId=${product.id}", e)
                    val rollbackIdx = lines.indexOfFirst { it.productId == product.id && it.variant == variant }
                    if (rollbackIdx >= 0) {
                        if (prev == null) lines.removeAt(rollbackIdx)
                        else lines[rollbackIdx] = prev
                    }
                }
            }
        }
    }

    fun changeQty(productId: String, variant: String?, delta: Int) {
        val lines = cartFor(selectedClubId).productLines
        val idx = lines.indexOfFirst { it.productId == productId && it.variant == variant }
        Log.d("Cart", "changeQty: productId=$productId variant=$variant delta=$delta idx=$idx listSize=${lines.size}")
        if (idx < 0) {
            Log.w("Cart", "changeQty: item not found in list, returning")
            return
        }

        val cur = lines[idx]
        val newQty = (cur.qty + delta).coerceAtLeast(0)
        Log.d("Cart", "changeQty: curQty=${cur.qty} newQty=$newQty lineId=${cur.lineId}")

        // оптимистичное обновление — UI реагирует мгновенно
        if (newQty == 0) {
            lines.removeAt(idx)
            Log.d("Cart", "changeQty: removed item, listSize=${lines.size}")
        } else {
            lines[idx] = cur.copy(qty = newQty)
        }

        // после мутации auto-sync не должен перезаписывать локальное состояние
        localCartModified = true
        cartSyncJob?.cancel()
        cartSyncJob = null

        // берём clubId из самой строки, а не из selectedClubId — пользователь мог уже сменить клуб
        val clubIdLong = resolveClubIdLongOrNull(cur)
        val lineId = cur.lineId

        if (user != null && clubIdLong != null && lineId != null && variant == null) {
            viewModelScope.launch {
                try {
                    if (newQty <= 0) {
                        Log.d("Cart", "changeQty: calling server DELETE lineId=$lineId")
                        cartRepo.deleteProductLine(clubIdLong, lineId)
                        Log.d("Cart", "changeQty: server DELETE success")
                    } else {
                        cartRepo.updateQty(clubIdLong, lineId, newQty)
                    }
                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Cart", "changeQty server failed: lineId=$lineId qty=$newQty", e)
                    if (newQty > 0) {
                        val rollbackIdx = lines.indexOfFirst { it.productId == productId && it.variant == variant }
                        if (rollbackIdx >= 0) lines[rollbackIdx] = cur
                    } else {
                        // восстанавливаем удалённый товар при ошибке сервера
                        val insertAt = idx.coerceIn(0, lines.size)
                        lines.add(insertAt, cur)
                    }
                }
            }
        } else {
            Log.d("Cart", "changeQty: skipping server call user=${user != null} club=$clubIdLong lineId=$lineId variant=$variant")
        }
    }

    /** Очистить корзину ОДНОГО клуба (по умолчанию — текущего). */
    fun clearCart(clubId: String = selectedClubId, onError: () -> Unit = {}) {
        val clubIdLong = (clubId.toLongOrNull() ?: clubId.removePrefix("c").toLongOrNull())
        if (user != null && clubIdLong != null) {
            viewModelScope.launch {
                try {
                    cartRepo.clear(clubIdLong)
                    // очищаем локальное состояние только после подтверждения сервером
                    cartFor(clubId).productLines.clear()
                    cartFor(clubId).bookingLines.clear()
                    if (clubId == selectedClubId) {
                        bookingDraft = BookingDraft(clubId = selectedClubId)
                        editingBookingId = null
                    }
                } catch (_: Exception) {
                    onError()
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

    data class CommitResult(val ok: Boolean, val message: String? = null)

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

                // брони, которые есть на сервере, но уже удалены пользователем локально —
                // удаляем их перед добавлением новой, иначе сервер вернёт 400 (конфликт слота)
                val localBookingIds = cartFor(selectedClubId).bookingLines.map { it.id }.toSet()
                before.bookings
                    .filter { it.lineId.toString() !in localBookingIds }
                    .forEach { runCatching { cartRepo.deleteBookingLine(clubIdLong, it.lineId) } }

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

                // добавляем только новую бронь в локальный список, не трогая продукты
                // и ранее удалённые брони — полная замена через applyServerCart* восстанавливала бы их
                val newDto = afterSeats.bookings.firstOrNull { it.lineId == newLineId }
                val bookingLines = cartFor(selectedClubId).bookingLines
                if (editingIdLong != null) {
                    bookingLines.removeAll { it.id == editingIdLong.toString() }
                }
                if (newDto != null) {
                    val s = LocalDateTime.parse(newDto.startAt)
                    val e2 = LocalDateTime.parse(newDto.endAt)
                    val baseDate = s.toLocalDate()
                    bookingLines.add(
                        CartBookingLine(
                            id = newDto.lineId.toString(),
                            clubId = selectedClubId,
                            date = baseDate,
                            startDayOffset = 0,
                            startMin = s.hour * 60 + s.minute,
                            endDayOffset = ChronoUnit.DAYS.between(baseDate, e2.toLocalDate()).toInt().coerceAtLeast(0),
                            endMin = e2.hour * 60 + e2.minute,
                            packageHours = newDto.packageHours,
                            seatIds = newDto.seatIds.map { it.toString() }
                        )
                    )
                }

                editingBookingId = null
                bookingDraft = d.copy(selectedSeatIds = emptySet())
                onResult(CommitResult(true))
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    400 -> "Место уже занято на это время. Удали старую бронь из корзины и попробуй снова."
                    409 -> "Конфликт бронирования: это место уже занято."
                    else -> "Ошибка брони: ${e.code()}"
                }
                onResult(CommitResult(false, msg))
            } catch (e: Exception) {
                onResult(CommitResult(false, "Не удалось добавить бронь"))
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
        // сохраняем для возможного отката
        val removed = cartsByClub.values.flatMap { it.bookingLines }.firstOrNull { it.id == id }

        // оптимистичное удаление — UI реагирует мгновенно
        cartsByClub.values.forEach { it.bookingLines.removeAll { line -> line.id == id } }
        if (editingBookingId == id) editingBookingId = null

        // после мутации auto-sync не должен перезаписывать локальное состояние
        localCartModified = true
        cartSyncJob?.cancel()
        cartSyncJob = null

        // убираем освобождённые места из busySeatIds чтобы "Быстрая бронь" сразу видела их свободными
        if (removed != null) {
            val startDt = lineStartDateTime(removed)
            val endDt = lineEndDateTime(removed)
            val stillOccupied = cartFor(removed.clubId).bookingLines
                .flatMap { line ->
                    if (intervalsOverlap(startDt, endDt, lineStartDateTime(line), lineEndDateTime(line)))
                        line.seatIds else emptyList()
                }
                .toSet()
            busySeatIds = busySeatIds - (removed.seatIds.toSet() - stillOccupied)
        }

        // берём clubId из самой брони, а не из selectedClubId — пользователь мог уже сменить клуб
        val clubIdLong = removed?.clubId?.toLongOrNull()
            ?: removed?.clubId?.removePrefix("c")?.toLongOrNull()
            ?: selectedClubIdLongOrNull()
        val lineIdLong = id.toLongOrNull()

        if (user != null && clubIdLong != null && lineIdLong != null) {
            viewModelScope.launch {
                try {
                    cartRepo.deleteBookingLine(clubIdLong, lineIdLong)
                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // не откатываем: пользователь хотел удалить бронь,
                    // синхронизация восстановит при необходимости
                    Log.e("Cart", "removeBooking failed: lineId=$lineIdLong", e)
                }
            }
        }
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

    /** Сброс пакета — переход на стандартный тариф без фиксированной длительности. */
    fun resetTimePackage() {
        bookingDraft = bookingDraft.copy(packageHours = null)
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
        val startDt = startDateTime(d)
        val endDt = endDateTime(d)

        // места, уже занятые бронями пользователя в корзине на это время
        val cartConflictSeats = cartFor(d.clubId).bookingLines
            .filter { line -> intervalsOverlap(startDt, endDt, lineStartDateTime(line), lineEndDateTime(line)) }
            .flatMap { it.seatIds }
            .toSet()

        // сортируем: сначала явно свободные по серверу, затем возможно освобождённые нами
        val free = clubSeats
            .filter { it.hasPc }
            .filter { it.id !in cartConflictSeats }
            .sortedBy { if (it.id in busySeatIds) 1 else 0 }
            .firstOrNull()
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
     * - данных занятости с сервера (busySeatIds)
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
                onError("Ошибка оплаты")
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
                historyError = "Ошибка загрузки истории"
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
                onError("Ошибка оплаты")
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
                onError("Ошибка отмены")
            }
        }
    }

    private fun PurchaseDetailsDto.toPurchase(clubs: List<Club>): Purchase {
        val clubName = clubName.ifBlank { clubs.firstOrNull { it.id == clubId.toString() }?.name ?: "Клуб" }
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
                packageHours = b.packageHours,
                rateRubPerHour = b.rateRubPerHourSnapshot,
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

    fun startDateTime(d: BookingDraft): LocalDateTime =
        d.date.plusDays(d.startDayOffset.toLong()).atStartOfDay().plusMinutes(d.startMin.toLong())

    fun endDateTime(d: BookingDraft): LocalDateTime =
        d.date.plusDays(d.endDayOffset.toLong()).atStartOfDay().plusMinutes(d.endMin.toLong())

    /**
     * Диапазон времени для подсветки занятости мест.
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

    /** Загружает избранные клубы с сервера. Вызывать после авторизации. */
    fun loadFavorites() {
        if (user == null) return
        viewModelScope.launch {
            try {
                val ids = favoritesRepo.getFavorites().map { it.toString() }
                favoriteClubIds.clear()
                favoriteClubIds.addAll(ids)
            } catch (_: Exception) {
                // не критично — избранное просто не покажется
            }
        }
    }

    /** Оптимистично переключает избранное и синхронизирует с сервером. */
    fun toggleFavoriteClub(clubId: String) {
        val isNowFavorite = favoriteClubIds.contains(clubId)
        // оптимистичное обновление UI
        if (isNowFavorite) favoriteClubIds.remove(clubId) else favoriteClubIds.add(clubId)

        if (user == null) return
        val clubIdLong = clubId.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                if (isNowFavorite) favoritesRepo.removeFavorite(clubIdLong)
                else favoritesRepo.addFavorite(clubIdLong)
            } catch (_: Exception) {
                // откат при ошибке
                if (isNowFavorite) favoriteClubIds.add(clubId) else favoriteClubIds.remove(clubId)
            }
        }
    }

    fun submitReport(
        clubId: String,
        message: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val clubIdLong = clubId.toLongOrNull() ?: return onError("Неверный ID клуба")
        viewModelScope.launch {
            try {
                reportRepo.submitReport(clubIdLong, message)
                onSuccess()
            } catch (e: Exception) {
                onError("Не удалось отправить сообщение")
            }
        }
    }

    fun chooseClub(clubId: String) {
        setClub(clubId)
        confirmClub()

        if (user != null) {
            loadShopData(force = true)
            loadTimePackages(force = true)
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
     * null => стандартный тариф (первый в списке или дефолтный).
     */
    fun bookingRateRubPerHour(packageHours: Int?): Int {
        if (packageHours != null) {
            val pkg = timePackages.firstOrNull { it.hours == packageHours }
            if (pkg != null) return pkg.pricePerHourRub
        }
        // стандартный тариф: минимальная цена места → первый пакет → константа
        return standardRateRubPerHour ?: timePackages.firstOrNull()?.pricePerHourRub ?: 0
    }

    /**
     * Эффективная ставка ₽/ч для конкретного типа места с учётом выбранного пакета.
     *
     * Формула: baseRate + max(0, seatTypePrice - standardRate)
     * Пример: пакет 80 ₽/ч, стандарт 100 ₽/ч, VIP 110 ₽/ч → VIP = 80 + (110-100) = 90 ₽/ч
     */
    fun effectiveRateForSeatType(seatType: com.example.computerclub.model.SeatType): Int {
        val standard = standardRateRubPerHour ?: 0
        val baseRate = bookingRateRubPerHour(bookingDraft.packageHours)
        val seatTypePrice = seatPrices.firstOrNull { it.seatType == seatType.name }?.pricePerHourRub ?: standard
        val surcharge = maxOf(0, seatTypePrice - standard)
        return baseRate + surcharge
    }

    /** Итоговая стоимость бронирования для набора выбранных мест. */
    fun bookingTotalRub(selectedSeatIds: Set<String>): Int {
        if (selectedSeatIds.isEmpty()) return 0
        val durationHours = bookingDurationHours(bookingDraft)
        return selectedSeatIds.sumOf { seatId ->
            val seat = clubSeats.firstOrNull { it.id == seatId }
            val rate = if (seat != null) effectiveRateForSeatType(seat.type) else bookingRateRubPerHour(bookingDraft.packageHours)
            ceil(durationHours * rate).toInt()
        }
    }

    private fun bookingDurationHours(d: BookingDraft): Double {
        val startAbs = d.startDayOffset * DAY_MIN + d.startMin
        val endAbs = d.endDayOffset * DAY_MIN + d.endMin
        return (endAbs - startAbs).coerceAtLeast(0) / 60.0
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
                                imageUrl = dto.imageUrl?.let { if (it.startsWith("/")) "$baseUrl$it" else it },
                                latitude = dto.latitude,
                                longitude = dto.longitude,
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
                                description = dto.description ?: "",
                                imageUrl = dto.imageUrl?.let { if (it.startsWith("/")) "$baseUrl$it" else it },
                                latitude = dto.latitude,
                                longitude = dto.longitude
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
                            description = dto.description ?: "",
                            imageUrl = dto.imageUrl?.let { if (it.startsWith("/")) "$baseUrl$it" else it },
                            latitude = dto.latitude,
                            longitude = dto.longitude
                        )
                    }
                }

                clubs = loaded
                ensureSelectedClubValid()
            } catch (e: Exception) {
                clubsError = "Не удалось загрузить клубы"
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
