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
import androidx.compose.material.icons.filled.Refresh
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
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Seat
import com.example.computerclub.model.SeatAvailability
import com.example.computerclub.model.SeatType
import com.example.computerclub.model.TimeRange
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class SeatPosFrac(
    val seatId: String,
    val number: String,
    val x: Float,   // 0..1
    val y: Float,   // 0..1
    val size: Float // доля от ширины контейнера
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookingSeatsScreen(
    appVm: AppViewModel,
    onGoToCart: () -> Unit
) {
    val clubId = appVm.selectedClubId
    val club = remember(clubId) { FakeData.clubs.firstOrNull { it.id == clubId } }
    val seats: List<Seat> = remember(clubId) { FakeData.seatMapByClub[clubId].orEmpty() }

    if (club == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Сначала выбери клуб.") }
        return
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

    val layout = remember(seats) { buildLayoutPercent(seats) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize().padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 2.dp,          // ✅ ближе к верхней панели
                    bottom = padding.calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // --- Верх: клуб + время + избранное (поднято выше) ---
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
                    modifier = Modifier.size(40.dp) // чуть компактнее
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Избранное"
                    )
                }
            }

            // ✅ заголовок тоже ближе к верху
            Text("Выбор мест", style = MaterialTheme.typography.titleMedium)

            // --- Карта растянута на максимум ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // карта
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // карта занимает максимум в своём блоке
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
                            val availability = appVm.seatAvailability(seat, range)

                            val seatSizeDp = (planW.value * p.size).dp.coerceIn(28.dp, 42.dp)

                            SeatSquare(
                                modifier = Modifier
                                    .offset(
                                        x = (planW.value * p.x).dp,
                                        y = (planH.value * p.y).dp
                                    )
                                    .size(seatSizeDp)
                                    .combinedClickable(
                                        onClick = {
                                            if (availability == SeatAvailability.BOOKED) {
                                                scope.launch { snackbar.showSnackbar("Место занято на выбранное время") }
                                                return@combinedClickable
                                            }
                                            appVm.toggleSeat(seat.id)
                                            if (!selectedSeat && availability == SeatAvailability.PARTIAL) {
                                                scope.launch {
                                                    snackbar.showSnackbar("Место может быть недоступно из-за возможного продления")
                                                }
                                            }
                                        },
                                        onLongClick = { seatInfo = "${seat.label}\n${seat.equipment}" }
                                    ),
                                number = p.number,
                                kind = when {
                                    selectedSeat -> SeatKind.SELECTED
                                    availability == SeatAvailability.BOOKED -> SeatKind.BOOKED
                                    availability == SeatAvailability.PARTIAL -> SeatKind.PARTIAL
                                    else -> SeatKind.FREE
                                }
                            )
                        }
                    }
                }

                // ✅ КНОПКИ НИЖЕ СХЕМЫ (не внутри карты) и меньше
                MapControlsCompact(
                    modifier = Modifier.fillMaxWidth(),
                    onZoomOut = ::zoomOut,
                    onReset = ::resetView,
                    onZoomIn = ::zoomIn,
                    onRefresh = { scope.launch { snackbar.showSnackbar("Обновлено") } }
                )
            }

            // --- Низ ---
            Text(
                text = selectedSeatsLabel(draft.selectedSeatIds, seats),
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = onGoToCart,
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

    if (seatInfo != null) {
        AlertDialog(
            onDismissRequest = { seatInfo = null },
            confirmButton = { TextButton(onClick = { seatInfo = null }) { Text("Ок") } },
            title = { Text("Информация о месте") },
            text = { Text(seatInfo!!) }
        )
    }
}

// ---------------- фон ----------------

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

// ---------------- места ----------------

private enum class SeatKind { FREE, PARTIAL, BOOKED, SELECTED }

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
        SeatKind.PARTIAL -> Triple(cs.tertiaryContainer, cs.tertiary, cs.onTertiaryContainer)
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

// ---------------- панель управления (компактная, ниже карты) ----------------

@Composable
private fun MapControlsCompact(
    modifier: Modifier,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onZoomIn: () -> Unit,
    onRefresh: () -> Unit
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallSquareButtonCompact(text = "−", onClick = onZoomOut)
                SmallSquareButtonCompact(text = "•", onClick = onReset)
                SmallSquareButtonCompact(text = "+", onClick = onZoomIn)
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
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

// ---------------- координаты мест (проценты) ----------------

private fun buildLayoutPercent(seats: List<Seat>): List<SeatPosFrac> {
    fun seatNumber(label: String): String = label.substringAfterLast('-').trim()

    val vip = seats.filter { it.type == SeatType.VIP }.sortedBy { it.label }
    val reg = seats.filter { it.type != SeatType.VIP }.sortedBy { it.label }

    val out = mutableListOf<SeatPosFrac>()
    val seatSize = 0.10f

    // VIP: 4 места
    vip.take(4).forEachIndexed { i, s ->
        out += SeatPosFrac(
            seatId = s.id,
            number = seatNumber(s.label),
            x = 0.18f + i * 0.14f,
            y = 0.14f,
            size = seatSize
        )
    }

    // STANDARD: 7 колонок
    reg.forEachIndexed { i, s ->
        val col = i % 7
        val row = i / 7
        out += SeatPosFrac(
            seatId = s.id,
            number = seatNumber(s.label),
            x = 0.16f + col * 0.11f,
            y = 0.45f + row * 0.12f,
            size = seatSize
        )
    }

    return out
}

// ---------------- строка выбранных мест ----------------

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

// ---------------- строка времени ----------------

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
