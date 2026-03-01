package com.example.computerclub.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.computerclub.model.Seat
import com.example.computerclub.model.SeatAvailability
import com.example.computerclub.model.SeatType
import com.example.computerclub.model.FloorplanSeatPos
import com.example.computerclub.model.TimeRange
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private const val DAY_MIN = 24 * 60

// UI: только 3 состояния (свободно / занято / выбрано)
private enum class SeatKind { FREE, BOOKED, SELECTED }

private enum class SeatsSheetKind { INFO, MAX_TIME }

private data class SeatMaxTimeRow(
    val seat: Seat,
    val maxMin: Int,          // сколько минут максимум можно держать от выбранного старта
    val untilLabel: String,   // до какого времени
    val maxLabel: String      // формат “1д 2ч 30м”
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookingSeatsScreen(
    appVm: AppViewModel,
    onGoToCart: () -> Unit
) {
    val clubId = appVm.selectedClubId
    val club = remember(clubId, appVm.clubs) { appVm.clubs.firstOrNull { it.id == clubId } }
    val seats: List<Seat> = appVm.clubSeats

    if (club == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Сначала выбери клуб.") }
        return
    }

    // загружаем места и доступность с сервера
    LaunchedEffect(clubId, appVm.user) {
        appVm.loadSeatsAndAvailability(force = true)
    }

    // обновляем доступность при смене интервала
    LaunchedEffect(appVm.bookingDraft.startDayOffset, appVm.bookingDraft.startMin, appVm.bookingDraft.endDayOffset, appVm.bookingDraft.endMin) {
        appVm.loadSeatsAndAvailability(force = false)
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isFav = appVm.isFavoriteClub(club.id)

    val draft = appVm.bookingDraft
    val startDt = appVm.startDateTime(draft)
    val endDt = appVm.endDateTime(draft)
    val range: TimeRange? = appVm.selectedTimeRangeForSeats(draft)
    val durationMin = remember(startDt, endDt) {
        Duration.between(startDt, endDt).toMinutes().coerceAtLeast(0).toInt()
    }

    var seatInfo by remember { mutableStateOf<String?>(null) }

    // zoom/pan
    var userScale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    fun zoomIn() { userScale = (userScale * 1.15f).coerceIn(0.8f, 3.5f) }
    fun zoomOut() { userScale = (userScale / 1.15f).coerceIn(0.8f, 3.5f) }
    fun resetView() { userScale = 1f; pan = Offset.Zero }

    // layout берём из опубликованной схемы (если есть) — иначе fallback на алгоритм по умолчанию
    val layout: List<FloorplanSeatPos> = remember(seats, appVm.floorplanSeats) {
        if (appVm.floorplanSeats.isNotEmpty()) appVm.floorplanSeats else buildLayoutPercent(seats)
    }

    // --- нижние шторки ---
    var sheetKind by remember { mutableStateOf<SeatsSheetKind?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- подготовка данных для шторки “макс. время” ---
    val startMinOfDay = draft.startMin // достаточно минут дня (FakeData без даты)
    val maxTimeRows by remember(seats, startMinOfDay, appVm.busySeatIds) {
        mutableStateOf(buildMaxTimeRows(seats, startMinOfDay, appVm.busySeatIds))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 2.dp,
                    bottom = padding.calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // --- Верх: клуб + время + избранное ---
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = club.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = headerLine(startDt, endDt, durationMin),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { appVm.toggleFavoriteClub(club.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Избранное"
                    )
                }
            }

            Text("Выбор мест", style = MaterialTheme.typography.titleMedium)

            // --- Карта ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(18.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val planW = maxWidth
                    val planH = maxHeight

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = userScale
                                scaleY = userScale
                                translationX = pan.x
                                translationY = pan.y
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, panChange, zoom, _ ->
                                    userScale = (userScale * zoom).coerceIn(0.8f, 3.5f)
                                    pan += panChange
                                }
                            }
                    ) {
                        HallBackgroundFill()

                        layout.forEach { p ->
                            val seat = seats.firstOrNull { it.id == p.seatId } ?: return@forEach
                            val selectedSeat = draft.selectedSeatIds.contains(seat.id)
                            // доступность приходит с сервера: любое занятое = BOOKED
                            val isBooked = appVm.busySeatIds.contains(seat.id)

                            val seatW = (planW.value * p.w).dp.coerceIn(28.dp, 64.dp)
                            val seatH = (planH.value * p.h).dp.coerceIn(28.dp, 64.dp)

                            val baseModifier = Modifier
                                .offset(
                                    x = (planW.value * p.x).dp,
                                    y = (planH.value * p.y).dp
                                )
                                .size(width = seatW, height = seatH)

                            // занятые места не кликабельны; выбранные можно снять
                            val clickModifier = if (!isBooked || selectedSeat) {
                                baseModifier.combinedClickable(
                                    onClick = { appVm.toggleSeat(seat.id) },
                                    onLongClick = { seatInfo = "${seat.label}\n${seat.equipment}" }
                                )
                            } else {
                                baseModifier
                            }

                            SeatSquare(
                                modifier = clickModifier,
                                number = seatNumber(seat.label),
                                kind = when {
                                    selectedSeat -> SeatKind.SELECTED
                                    isBooked -> SeatKind.BOOKED
                                    else -> SeatKind.FREE
                                }
                            )
                        }
                    }
                }

                // --- Кнопки под схемой ---
                MapControlsCompact(
                    modifier = Modifier.fillMaxWidth(),
                    onZoomOut = ::zoomOut,
                    onReset = ::resetView,
                    onZoomIn = ::zoomIn,
                    onInfo = { sheetKind = SeatsSheetKind.INFO },
                    onMaxTime = { sheetKind = SeatsSheetKind.MAX_TIME }
                )
            }

            // --- Низ ---
            Text(
                text = selectedSeatsLabel(draft.selectedSeatIds, seats),
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = {
                    appVm.commitCurrentBookingToCartAsync { res ->
                        if (res.ok) {
                            onGoToCart()
                        } else {
                            scope.launch {
                                snackbar.showSnackbar(res.message ?: "Не удалось добавить в корзину")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = draft.selectedSeatIds.isNotEmpty(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("В корзину", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // --- Шторки снизу ---
    if (sheetKind != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetKind = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (sheetKind) {
                SeatsSheetKind.INFO -> SeatsInfoSheet()
                SeatsSheetKind.MAX_TIME -> SeatsMaxTimeSheet(maxTimeRows)
                null -> Unit
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    // --- Диалог long-press по месту ---
    if (seatInfo != null) {
        AlertDialog(
            onDismissRequest = { seatInfo = null },
            confirmButton = { TextButton(onClick = { seatInfo = null }) { Text("Ок") } },
            title = { Text("Информация о месте") },
            text = { Text(seatInfo!!) }
        )
    }
}

// --- Шторки ---

@Composable
private fun SeatsInfoSheet() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Информация о компьютерах", style = MaterialTheme.typography.titleLarge)

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("СТАНДАРТ", style = MaterialTheme.typography.titleMedium)
                SpecLine("Процессор", "Intel Core i5 / Ryzen 5")
                SpecLine("Видеокарта", "RTX 3060 / RX 6600")
                SpecLine("ОЗУ", "16 ГБ DDR4")
                SpecLine("Накопитель", "SSD 512 ГБ")
                SpecLine("Монитор", "24\" 144 Гц")
                SpecLine("Периферия", "Механика + игровая мышь")
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("VIP", style = MaterialTheme.typography.titleMedium)
                SpecLine("Процессор", "Intel Core i7 / Ryzen 7")
                SpecLine("Видеокарта", "RTX 4070 / RX 7800 XT")
                SpecLine("ОЗУ", "32 ГБ DDR5")
                SpecLine("Накопитель", "NVMe SSD 1 ТБ")
                SpecLine("Монитор", "27\" 240 Гц")
                SpecLine("Периферия", "Топ-гарнитура + премиум мышь")
            }
        }
    }
}

@Composable
private fun SpecLine(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(right, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun SeatsMaxTimeSheet(rows: List<SeatMaxTimeRow>) {
    val vip = remember(rows) { rows.filter { it.seat.type == SeatType.VIP }.sortedByDescending { it.maxMin } }
    val std = remember(rows) { rows.filter { it.seat.type != SeatType.VIP }.sortedByDescending { it.maxMin } }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Максимально доступное время", style = MaterialTheme.typography.titleLarge)

        if (vip.isNotEmpty()) {
            Text("VIP", style = MaterialTheme.typography.titleMedium)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    vip.forEach { r -> MaxTimeRow(r) }
                }
            }
        }

        if (std.isNotEmpty()) {
            Text("СТАНДАРТ", style = MaterialTheme.typography.titleMedium)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    std.forEach { r -> MaxTimeRow(r) }
                }
            }
        }
    }
}

@Composable
private fun MaxTimeRow(r: SeatMaxTimeRow) {
    val unavailable = r.maxMin <= 0
    val cs = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                r.seat.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = if (unavailable) "Недоступно на выбранный старт" else "Осталось: ${r.maxLabel}"
            Text(
                sub,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unavailable) cs.error else cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (unavailable) "—" else "До ${r.untilLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

// --- Фон ---

@Composable
private fun HallBackgroundFill() {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(Modifier.fillMaxSize().padding(12.dp)) {
        Box(
            Modifier.fillMaxSize()
                .border(1.dp, outline, RoundedCornerShape(16.dp))
        )

        // VIP блок
        Box(
            Modifier
                .fillMaxWidth(0.62f)
                .fillMaxHeight(0.25f)
                .offset(10.dp, 10.dp)
                .border(1.dp, outline, RoundedCornerShape(14.dp))
        )
        Text(
            "VIP",
            modifier = Modifier
                .offset(10.dp, 14.dp)
                .fillMaxWidth(0.62f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )

        // STANDARD блок
        Box(
            Modifier
                .fillMaxWidth(0.80f)
                .fillMaxHeight(0.58f)
                .offset(10.dp, 92.dp)
                .border(1.dp, outline, RoundedCornerShape(14.dp))
        )
        Text(
            "STANDARD",
            modifier = Modifier
                .offset(10.dp, 96.dp)
                .fillMaxWidth(0.80f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )

        // STAFF справа
        Box(
            Modifier
                .fillMaxWidth(0.18f)
                .fillMaxHeight(0.58f)
                .align(Alignment.CenterEnd)
                .offset((-10).dp, 62.dp)
                .border(1.dp, outline, RoundedCornerShape(14.dp))
        )
        Text(
            "STAFF",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset((-10).dp, 66.dp)
                .fillMaxWidth(0.18f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
    }
}

// --- Места ---

@Composable
private fun SeatSquare(
    modifier: Modifier,
    number: String,
    kind: SeatKind
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)

    val (bg, border, textColor) = when (kind) {
        SeatKind.SELECTED -> Triple(cs.primary, cs.primary, cs.onPrimary)
        SeatKind.BOOKED -> Triple(cs.surfaceVariant, cs.outlineVariant, cs.onSurfaceVariant)
        SeatKind.FREE -> Triple(cs.surface, cs.outline, cs.onSurface)
    }

    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .size(width = 18.dp, height = 10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    if (kind == SeatKind.SELECTED) cs.onPrimary.copy(alpha = 0.9f)
                    else cs.onSurface.copy(alpha = 0.12f)
                )
        )
        Text(
            text = number,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

// --- Панель управления ---

@Composable
private fun MapControlsCompact(
    modifier: Modifier,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onZoomIn: () -> Unit,
    onInfo: () -> Unit,
    onMaxTime: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // слева — управление масштабом
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallSquareButtonCompact(text = "−", onClick = onZoomOut)
                SmallSquareButtonCompact(text = "•", onClick = onReset)
                SmallSquareButtonCompact(text = "+", onClick = onZoomIn)
            }

            Spacer(Modifier.weight(1f))

            // справа — инфо и список времени
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallSquareButtonCompact(text = "i", onClick = onInfo)
                SmallSquareButtonCompact(text = "⏱", onClick = onMaxTime)
            }
        }
    }
}

@Composable
private fun SmallSquareButtonCompact(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

// --- Координаты мест ---

private fun seatNumber(label: String): String = label.substringAfterLast('-').trim()

private fun buildLayoutPercent(seats: List<Seat>): List<FloorplanSeatPos> {

    val vip = seats.filter { it.type == SeatType.VIP }.sortedBy { it.label }
    val reg = seats.filter { it.type != SeatType.VIP }.sortedBy { it.label }

    val out = mutableListOf<FloorplanSeatPos>()
    val seatW = 0.10f
    val seatH = 0.10f

    // VIP: 4 места
    vip.take(4).forEachIndexed { i, s ->
        out += FloorplanSeatPos(
            seatId = s.id,
            x = 0.18f + i * 0.14f,
            y = 0.14f,
            w = seatW,
            h = seatH
        )
    }

    // STANDARD: 7 колонок
    reg.forEachIndexed { i, s ->
        val col = i % 7
        val row = i / 7
        out += FloorplanSeatPos(
            seatId = s.id,
            x = 0.16f + col * 0.11f,
            y = 0.45f + row * 0.12f,
            w = seatW,
            h = seatH
        )
    }

    return out
}

// --- Строка выбранных мест ---

private fun selectedSeatsLabel(selectedIds: Set<String>, seats: List<Seat>): String {
    if (selectedIds.isEmpty()) return "Вы выбрали 0 мест"

    val picked = seats.filter { it.id in selectedIds }

    fun seatNumber(s: Seat): Int? =
        s.label.substringAfterLast('-').trim().toIntOrNull()

    fun formatGroup(title: String, nums: List<Int>): String {
        if (nums.isEmpty()) return ""
        val sorted = nums.sorted()
        val first = sorted.first()
        val rest = sorted.drop(1)
        return if (rest.isEmpty()) "$title #$first" else "$title #$first, ${rest.joinToString(", ")}"
    }

    val regularNums = picked.filter { it.type != SeatType.VIP }.mapNotNull(::seatNumber)
    val vipNums = picked.filter { it.type == SeatType.VIP }.mapNotNull(::seatNumber)

    val parts = listOf(
        formatGroup("СТАНДАРТ", regularNums),
        formatGroup("VIP", vipNums)
    ).filter { it.isNotBlank() }

    return "Вы выбрали место: ${parts.joinToString(" ")}."
}

// --- Строка времени ---

private fun headerLine(startDt: LocalDateTime, endDt: LocalDateTime, durationMin: Int): String {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM")
    val dowFmt = DateTimeFormatter.ofPattern("EEE", Locale("ru"))
    val dur = formatDuration(durationMin)
    return "${startDt.format(timeFmt)}–${endDt.format(timeFmt)}, ${startDt.format(dateFmt)} [${startDt.format(dowFmt).lowercase()}] • $dur"
}

private fun formatDuration(deltaMin: Int): String {
    val d = deltaMin.coerceAtLeast(0)
    val h = d / 60
    val m = d % 60
    return if (h > 0) "${h}ч ${m}м" else "${m}м"
}

// --- Логика max-time (мок) ---

private fun buildMaxTimeRows(
    seats: List<Seat>,
    startMinOfDay: Int,
    busySeatIds: Set<String>
): List<SeatMaxTimeRow> {
    // горизонт: 3 суток, чтобы можно было показать “> дня”
    val horizonMin = 3 * DAY_MIN

    return seats.map { seat ->
        // сервер даёт занятость только для выбранного интервала — max-time считаем упрощённо:
        // если место занято — 0, иначе показываем полный горизонт
        val maxMin = if (seat.id in busySeatIds) 0 else horizonMin

        val maxLabel = if (maxMin <= 0) "0м" else formatDurationWithDays(maxMin)
        val untilLabel = if (maxMin <= 0) "—" else formatUntilTime(startMinOfDay, maxMin)

        SeatMaxTimeRow(
            seat = seat,
            maxMin = maxMin,
            untilLabel = untilLabel,
            maxLabel = maxLabel
        )
    }
}

/**
 * Вычисляет максимальную непрерывную свободную полосу от startMin до ближайшей брони.
 * Брони могут быть “wrap” (23:30–01:00) — разворачиваем в сегменты и дублируем на
 * следующие сутки, чтобы поймать ситуацию: старт 23:30, бронь 00:00–01:00.
 */
private fun maxContinuousFreeFromStart(
    startMin: Int,
    booked: List<TimeRange>,
    horizonMin: Int
): Int {
    if (booked.isEmpty()) return horizonMin

    fun segments(r: TimeRange): List<Pair<Int, Int>> {
        return if (r.endMin >= r.startMin) listOf(r.startMin to r.endMin)
        else listOf(r.startMin to DAY_MIN, 0 to r.endMin)
    }

    val baseSeg = booked.flatMap(::segments)
        .filter { (s, e) -> s != e }
        .map { (s, e) -> s.coerceIn(0, DAY_MIN) to e.coerceIn(0, DAY_MIN) }

    if (baseSeg.isEmpty()) return horizonMin

    fun inside(t: Int, seg: Pair<Int, Int>) = t >= seg.first && t < seg.second
    if (baseSeg.any { inside(startMin, it) }) return 0

    // мок: бронь может продлиться до +2 часов от конца — считаем место недоступным в зоне риска
    val extSeg = baseSeg.flatMap { (s, e) ->
        val e2 = e + 120
        if (e2 <= DAY_MIN) listOf(s to e2)
        else listOf(s to DAY_MIN, 0 to (e2 - DAY_MIN))
    }
    if (extSeg.any { inside(startMin, it) }) return 0

    // дублируем на несколько суток вперёд
    val days = ceil(horizonMin.toDouble() / DAY_MIN.toDouble()).toInt().coerceAtLeast(1)
    val all = mutableListOf<Pair<Int, Int>>()
    for (d in 0..days) {
        val shift = d * DAY_MIN
        baseSeg.forEach { (s, e) ->
            all += (s + shift) to (e + shift)
        }
    }

    val startAbs = startMin // считаем от “дня 0”
    val limit = startAbs + horizonMin

    val nextStart = all
        .asSequence()
        .filter { (s, _) -> s >= startAbs }
        .map { it.first }
        .minOrNull()

    if (nextStart == null || nextStart > limit) return horizonMin
    return (nextStart - startAbs).coerceAtLeast(0)
}

private fun formatDurationWithDays(min: Int): String {
    val m = min.coerceAtLeast(0)
    val d = m / DAY_MIN
    val rem = m % DAY_MIN
    val h = rem / 60
    val mm = rem % 60

    return buildString {
        if (d > 0) append("${d}д ")
        if (h > 0) append("${h}ч ")
        append("${mm}м")
    }
}

private fun formatUntilTime(startMinOfDay: Int, addMin: Int): String {
    val abs = startMinOfDay + addMin
    val dayOffset = abs / DAY_MIN
    val m = abs % DAY_MIN
    val hh = (m / 60).toString().padStart(2, '0')
    val mm = (m % 60).toString().padStart(2, '0')
    return if (dayOffset > 0) "$hh:$mm (+${dayOffset}д)" else "$hh:$mm"
}
