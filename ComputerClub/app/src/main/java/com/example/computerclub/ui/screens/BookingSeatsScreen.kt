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
import com.example.computerclub.model.FloorplanFloorPos
import com.example.computerclub.model.FloorplanWallPos
import com.example.computerclub.model.WallOrientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.example.computerclub.model.TimeRange
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private const val DAY_MIN = 24 * 60

private val SEATS_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val SEATS_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM")
private val SEATS_DOW_FMT = DateTimeFormatter.ofPattern("EEE", Locale("ru"))

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

    val isFav by remember { derivedStateOf { appVm.isFavoriteClub(club.id) } }

    val draft = appVm.bookingDraft
    val startDt = appVm.startDateTime(draft)
    val endDt = appVm.endDateTime(draft)
    val range: TimeRange? = appVm.selectedTimeRangeForSeats(draft)
    val durationMin = remember(startDt, endDt) {
        Duration.between(startDt, endDt).toMinutes().coerceAtLeast(0).toInt()
    }
    val headerText = remember(startDt, endDt, durationMin) { headerLine(startDt, endDt, durationMin) }

    var seatInfo by remember { mutableStateOf<String?>(null) }

    // zoom/pan
    var fitScale by remember { mutableStateOf(1f) }
    var fitPan by remember { mutableStateOf(Offset.Zero) }
    var userScale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var userHasZoomed by remember { mutableStateOf(false) }
    fun zoomIn() { userHasZoomed = true; userScale = (userScale * 1.15f).coerceIn(0.5f, 5f) }
    fun zoomOut() { userHasZoomed = true; userScale = (userScale / 1.15f).coerceIn(0.5f, 5f) }
    fun resetView() { userScale = fitScale; pan = fitPan; userHasZoomed = false }

    // layout берём из опубликованной схемы (если есть) — иначе fallback на алгоритм по умолчанию
    val layout: List<FloorplanSeatPos> = remember(seats, appVm.floorplanSeats) {
        if (appVm.floorplanSeats.isNotEmpty()) appVm.floorplanSeats else buildLayoutPercent(seats)
    }
    // Map для O(1) lookup вместо O(n) firstOrNull внутри layout.forEach
    val seatsById = remember(seats) { seats.associateBy { it.id } }
    val walls: List<FloorplanWallPos> = appVm.floorplanWalls
    val floor: List<FloorplanFloorPos> = appVm.floorplanFloor

    // --- нижние шторки ---
    var sheetKind by remember { mutableStateOf<SeatsSheetKind?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- подготовка данных для шторки “макс. время” ---
    val startMinOfDay = draft.startMin
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
                        text = headerText,
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
                        contentDescription = "Избранное",
                        tint = if (isFav) com.example.computerclub.ui.theme.BrandIndigo
                               else com.example.computerclub.ui.theme.TextMuted
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
                        .clip(com.example.computerclub.ui.theme.ShapeXL)
                        .border(
                            BorderStroke(1.dp, com.example.computerclub.ui.theme.AppBorder),
                            com.example.computerclub.ui.theme.ShapeXL
                        )
                ) {
                    val planW = maxWidth
                    val planH = maxHeight

                    val density = LocalDensity.current.density

                    // вычисляем масштаб, при котором весь контент помещается в контейнер
                    val computedFitScale = remember(layout, planW, planH) {
                        if (layout.isEmpty() || planW.value == 0f || planH.value == 0f) return@remember 1f
                        var maxRight = 0f
                        var maxBottom = 0f
                        layout.forEach { p ->
                            val nW = planW.value * p.w
                            val nH = planH.value * p.h
                            val sW = nW.coerceIn(56f, 96f)
                            val sH = nH.coerceIn(56f, 96f)
                            val sx = if (nW > 0f) sW / nW else 1f
                            val sy = if (nH > 0f) sH / nH else 1f
                            maxRight = maxOf(maxRight, planW.value * p.x * sx + sW)
                            maxBottom = maxOf(maxBottom, planH.value * p.y * sy + sH)
                        }
                        if (maxRight > 0f && maxBottom > 0f)
                            minOf(planW.value / maxRight, planH.value / maxBottom, 1f)
                        else 1f
                    }

                    // graphicsLayer масштабирует от центра контейнера —
                    // нужен pan-офсет чтобы содержимое (начинающееся от левого/верхнего края) стало видно
                    // panX = planW/2 * (scale - 1), переведено в пиксели
                    val computedFitPan = remember(computedFitScale, planW, planH, density) {
                        Offset(
                            planW.value * density / 2f * (computedFitScale - 1f),
                            planH.value * density / 2f * (computedFitScale - 1f)
                        )
                    }

                    // применяем автоматически, пока пользователь не начал зумировать вручную
                    LaunchedEffect(computedFitScale, computedFitPan) {
                        fitScale = computedFitScale
                        fitPan = computedFitPan
                        if (!userHasZoomed) {
                            userScale = computedFitScale
                            pan = computedFitPan
                        }
                    }

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
                                    userHasZoomed = true
                                    userScale = (userScale * zoom).coerceIn(0.5f, 5f)
                                    pan += panChange
                                }
                            }
                    ) {
                        // пол комнат — под стенами и местами
                        val numCols = appVm.floorplanNumCols
                        val numRows = appVm.floorplanNumRows
                        if ((floor.isNotEmpty() || walls.isNotEmpty()) && numCols > 0 && numRows > 0) {
                            val naturalCellW = planW.value / numCols
                            val naturalCellH = planH.value / numRows
                            val scaledCellW = naturalCellW.coerceIn(56f, 96f)
                            val scaledCellH = naturalCellH.coerceIn(56f, 96f)

                            floor.forEach { f ->
                                val bg = if (f.roomType == "VIP") Color(0xFFFFF3CD) else Color(0xFFF5F5F5)
                                Box(
                                    Modifier
                                        .offset(
                                            x = (f.col * scaledCellW).dp,
                                            y = (f.row * scaledCellH).dp
                                        )
                                        .size(width = scaledCellW.dp, height = scaledCellH.dp)
                                        .background(bg)
                                )
                            }
                        }
                        if (walls.isNotEmpty() && numCols > 0 && numRows > 0) {
                            val naturalCellW = planW.value / numCols
                            val naturalCellH = planH.value / numRows
                            val scaledCellW = naturalCellW.coerceIn(56f, 96f)
                            val scaledCellH = naturalCellH.coerceIn(56f, 96f)
                            val wallThickness = 4f

                            walls.forEach { w ->
                                when (w.orientation) {
                                    WallOrientation.H -> Box(
                                        Modifier
                                            .offset(
                                                x = (w.col * scaledCellW).dp,
                                                y = (w.row * scaledCellH - wallThickness / 2).dp
                                            )
                                            .size(width = scaledCellW.dp, height = wallThickness.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    )
                                    WallOrientation.V -> Box(
                                        Modifier
                                            .offset(
                                                x = (w.col * scaledCellW - wallThickness / 2).dp,
                                                y = (w.row * scaledCellH).dp
                                            )
                                            .size(width = wallThickness.dp, height = scaledCellH.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }

                        layout.forEach { p ->
                            val seat = seatsById[p.seatId] ?: return@forEach
                            val selectedSeat = draft.selectedSeatIds.contains(seat.id)
                            // доступность приходит с сервера: любое занятое = BOOKED
                            val isBooked = appVm.busySeatIds.contains(seat.id)

                            // естественный размер из схемы; если меньше 56dp — масштабируем и offset
                            val naturalW = planW.value * p.w
                            val naturalH = planH.value * p.h
                            val seatW = naturalW.coerceIn(56f, 96f).dp
                            val seatH = naturalH.coerceIn(56f, 96f).dp
                            // масштаб, применённый к размеру, применяем и к отступу — иначе места перекроются
                            val scaleX = if (naturalW > 0f) seatW.value / naturalW else 1f
                            val scaleY = if (naturalH > 0f) seatH.value / naturalH else 1f

                            val baseModifier = Modifier
                                .offset(
                                    x = (planW.value * p.x * scaleX).dp,
                                    y = (planH.value * p.y * scaleY).dp
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

            val selectedLabel = remember(draft.selectedSeatIds, seats) { selectedSeatsLabel(draft.selectedSeatIds, seats) }
            val bookingTotal = remember(draft.selectedSeatIds) {
                if (draft.selectedSeatIds.isNotEmpty()) appVm.bookingTotalRub(draft.selectedSeatIds) else null
            }

            // --- Низ ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (draft.selectedSeatIds.isNotEmpty() && bookingTotal != null) {
                    Text(
                        text = "${bookingTotal} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // подсказка о надбавке за VIP, если выбраны VIP-места
            val selectedSeats = remember(draft.selectedSeatIds, seats) {
                seats.filter { draft.selectedSeatIds.contains(it.id) }
            }
            val hasVip = selectedSeats.any { it.type == com.example.computerclub.model.SeatType.VIP }
            val hasRegular = selectedSeats.any { it.type != com.example.computerclub.model.SeatType.VIP }
            if (hasVip && appVm.seatPrices.isNotEmpty()) {
                val regularRate = appVm.effectiveRateForSeatType(com.example.computerclub.model.SeatType.REGULAR)
                val vipRate = appVm.effectiveRateForSeatType(com.example.computerclub.model.SeatType.VIP)
                val hint = if (hasRegular)
                    "Обычное: $regularRate ₽/ч · VIP: $vipRate ₽/ч"
                else
                    "VIP: $vipRate ₽/ч"
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            com.example.computerclub.ui.components.AppPrimaryButton(
                text = "В корзину",
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
                modifier = Modifier.fillMaxWidth(),
                enabled = draft.selectedSeatIds.isNotEmpty()
            )
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
                SeatsSheetKind.INFO -> SeatsInfoSheet(appVm.seatSpecs)
                SeatsSheetKind.MAX_TIME -> SeatsMaxTimeSheet(maxTimeRows)
                null -> Unit
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    // --- Диалог long-press по месту ---
    seatInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { seatInfo = null },
            confirmButton = { TextButton(onClick = { seatInfo = null }) { Text("Ок") } },
            title = { Text("Информация о месте") },
            text = { Text(info) }
        )
    }
}

// --- Шторки ---

@Composable
private fun SeatsInfoSheet(specs: List<com.example.computerclub.data.network.dto.SeatSpecResponseDto>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Информация о компьютерах", style = MaterialTheme.typography.titleLarge)

        if (specs.isEmpty()) {
            Text(
                "Информация не указана",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            specs.forEach { spec ->
                com.example.computerclub.ui.components.AppCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(spec.title, style = MaterialTheme.typography.titleMedium)
                        spec.specs.forEach { line ->
                            SpecLine(line.name, line.value)
                        }
                    }
                }
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
            com.example.computerclub.ui.components.AppCard {
                Column(Modifier.padding(vertical = 6.dp)) {
                    vip.forEach { r -> MaxTimeRow(r) }
                }
            }
        }

        if (std.isNotEmpty()) {
            Text("СТАНДАРТ", style = MaterialTheme.typography.titleMedium)
            com.example.computerclub.ui.components.AppCard {
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
        SeatKind.SELECTED -> Triple(
            com.example.computerclub.ui.theme.BrandIndigo,
            com.example.computerclub.ui.theme.BrandIndigo,
            Color.White
        )
        SeatKind.BOOKED -> Triple(
            com.example.computerclub.ui.theme.AppSurfaceAlt,
            com.example.computerclub.ui.theme.AppBorder,
            com.example.computerclub.ui.theme.TextMuted
        )
        SeatKind.FREE -> Triple(
            com.example.computerclub.ui.theme.BrandIndigoSoft,
            com.example.computerclub.ui.theme.BrandIndigo,
            com.example.computerclub.ui.theme.BrandIndigo
        )
    }

    val monitorColor = if (kind == SeatKind.SELECTED) Color.White.copy(alpha = 0.85f)
                       else com.example.computerclub.ui.theme.AppBorder

    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // монитор — показывает направление взгляда
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(monitorColor)
            )

            // номер места
            Text(
                text = number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(Modifier.height(4.dp))
        }
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
        shape = com.example.computerclub.ui.theme.ShapeLarge,
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
        shape = com.example.computerclub.ui.theme.ShapeSmall,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.computerclub.ui.theme.BrandIndigo),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.computerclub.ui.theme.AppBorder)
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
    val dur = formatDuration(durationMin)
    return "${startDt.format(SEATS_TIME_FMT)}–${endDt.format(SEATS_TIME_FMT)}, ${startDt.format(SEATS_DATE_FMT)} [${startDt.format(SEATS_DOW_FMT).lowercase()}] • $dur"
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
