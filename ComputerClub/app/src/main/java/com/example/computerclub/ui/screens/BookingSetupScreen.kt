@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.computerclub.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

private const val DAY_MIN = 24 * 60
private const val STEP = 30
private const val MIN_END_GAP = 60 // минимум 1 час

private val TIME_ITEM_WIDTH = 96.dp
private val TIME_ITEM_HEIGHT = 52.dp
private val TIME_ITEM_SPACING = 14.dp

private val DATE_TILE_SIZE = 48.dp

private val RU = Locale("ru")
private val DATE_TOP_FMT = DateTimeFormatter.ofPattern("d MMMM", RU)
private val DATE_TILE_DOW_FMT = DateTimeFormatter.ofPattern("EEE", RU)
private val DATE_RIGHT_FMT = DateTimeFormatter.ofPattern("d MMM", RU)

// окно “псевдо бесконечности”
private const val WHEEL_COUNT = 501
private const val WHEEL_MID = WHEEL_COUNT / 2

@Composable
fun BookingSetupScreen(
    appVm: AppViewModel,
    onNext: () -> Unit,
    onQuickBookToCart: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scopeSnack = rememberCoroutineScope()

    val draft = appVm.bookingDraft
    val club = remember(draft.clubId, appVm.clubs) {
        appVm.clubs.firstOrNull { it.id == draft.clubId }
            ?: FakeData.clubs.first { it.id == draft.clubId }
    }

    // загружаем места и доступность — нужно для "Быстрой брони"
    LaunchedEffect(draft.clubId, appVm.user) {
        appVm.loadSeatsAndAvailability(force = true)
    }
    LaunchedEffect(draft.startDayOffset, draft.startMin, draft.endDayOffset, draft.endMin) {
        appVm.loadSeatsAndAvailability(force = false)
    }

    // абсолютные минуты от draft.date
    val startAbsMin = draft.startDayOffset * DAY_MIN + draft.startMin
    val endAbsMin = draft.endDayOffset * DAY_MIN + draft.endMin
    val durationAbs = (endAbsMin - startAbsMin).coerceAtLeast(MIN_END_GAP)

    val startAbsNow by rememberUpdatedState(startAbsMin)
    val durationNow by rememberUpdatedState(durationAbs)

    fun minEndForStart(startAbs: Int) = startAbs + MIN_END_GAP

    fun setStartKeepingDuration(newStartAbsAligned: Int) {
        val newStartAbs = roundToStep(newStartAbsAligned)
        val delta = newStartAbs - startAbsNow
        if (delta == 0) return

        val newEndAbs = newStartAbs + durationNow
        val minEnd = minEndForStart(newStartAbs)
        val finalEnd = if (newEndAbs < minEnd) minEnd else newEndAbs

        appVm.shiftStartBy(delta)
        appVm.setEndAbsolute(finalEnd)
    }

    fun setEndWithMin(newEndAbsAligned: Int) {
        val newEndAbs = roundToStep(newEndAbsAligned)
        val minEnd = minEndForStart(startAbsNow)
        appVm.setEndAbsolute(if (newEndAbs < minEnd) minEnd else newEndAbs)
    }

    // --- Ограничение по времени телефона: нельзя выбрать прошедшее ---
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { now.value = LocalDateTime.now() }

    val minStartAbsLimit = remember(draft.date, now.value) {
        val n = now.value
        val today = n.toLocalDate()
        val curMin = n.hour * 60 + n.minute
        val rounded = ceilToStep(curMin)

        val minDate = if (rounded >= DAY_MIN) today.plusDays(1) else today
        val minMin = if (rounded >= DAY_MIN) 0 else rounded

        val daysBetween = ChronoUnit.DAYS.between(draft.date, minDate).toInt()
        daysBetween * DAY_MIN + minMin
    }

    // если старт оказался в прошлом — подтянем (и конец с сохранением длительности)
    LaunchedEffect(minStartAbsLimit, startAbsMin) {
        if (startAbsMin < minStartAbsLimit) {
            setStartKeepingDuration(minStartAbsLimit)
        }
    }

    // если конец оказался раньше старта+1ч — подтянем
    LaunchedEffect(startAbsMin, endAbsMin) {
        val minEnd = minEndForStart(startAbsMin)
        if (endAbsMin < minEnd) appVm.setEndAbsolute(minEnd)
    }

    // выбранная дата сверху = день начала
    val startDateSelected = remember(draft.date, draft.startDayOffset) {
        draft.date.plusDays(draft.startDayOffset.toLong())
    }

    // --- Календарь: запрет прошлых дат ---
    var showCalendar by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(startDateSelected) {
            startDateSelected.atStartOfDay(zone).toInstant().toEpochMilli()
        },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                return !d.isBefore(today) // прошедшие даты недопустимы
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year >= today.year
            }
        }
    )

    if (showCalendar) {
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        val deltaDays = ChronoUnit.DAYS.between(startDateSelected, picked).toInt()
                        if (deltaDays != 0) setStartKeepingDuration(startAbsNow + deltaDays * DAY_MIN)
                    }
                    showCalendar = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- КЛУБ ---
            Card {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(club.name, style = MaterialTheme.typography.titleMedium)
                        Text(club.address, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { appVm.toggleFavoriteClub(club.id) }) {
                        Icon(
                            imageVector = if (appVm.isFavoriteClub(club.id))
                                Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Избранное"
                        )
                    }
                }
            }

            // --- ДАТА (день начала) ---
            DateStrip(
                selectedDate = startDateSelected,
                onPick = { picked ->
                    // тут уже лента дат начинается от today, так что прошлое не попадёт
                    val deltaDays = ChronoUnit.DAYS.between(startDateSelected, picked).toInt()
                    if (deltaDays != 0) setStartKeepingDuration(startAbsNow + deltaDays * DAY_MIN)
                },
                onOpenCalendar = { showCalendar = true }
            )

            // --- НАЧАЛО ---
            InfiniteWindowTimeWheel(
                title = "Начало",
                baseDate = draft.date,
                currentAbsMin = startAbsMin,
                minLimitAbsMin = minStartAbsLimit,
                onCommitAbsMin = { pickedAbs -> setStartKeepingDuration(pickedAbs) }
            )

            Text(
                text = "Длительность: ${formatDuration(durationAbs)}",
                style = MaterialTheme.typography.titleLarge
            )

            // --- КОНЕЦ ---
            InfiniteWindowTimeWheel(
                title = "Конец",
                baseDate = draft.date,
                currentAbsMin = endAbsMin,
                minLimitAbsMin = startAbsMin + MIN_END_GAP,
                onCommitAbsMin = { pickedAbs -> setEndWithMin(pickedAbs) }
            )

            // --- ПАКЕТЫ ВРЕМЕНИ ---
            TimePackagesRow(
                selectedPackageHours = draft.packageHours,
                onPickPackageHours = { hours ->
                    // фиксируем длительность = пакет; конец подстраивается
                    appVm.applyTimePackage(hours)
                }
            )

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    val ok = appVm.quickBookOneSeat()
                    if (ok) {
                        // В корзину бронь попадает только после выбора места (быстрая бронь делает выбор автоматически)
                        appVm.commitCurrentBookingToCartAsync { res ->
                            if (res.ok) {
                                onQuickBookToCart()
                            } else {
                                scopeSnack.launch {
                                    snackbarHostState.showSnackbar(res.message ?: "Не удалось добавить в корзину")
                                }
                            }
                        }
                    } else {
                        scopeSnack.launch {
                            snackbarHostState.showSnackbar("Нет доступных компьютеров на выбранное время")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Быстрая бронь") }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Далее") }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )
    }
}

@Composable
private fun TimePackagesRow(
    selectedPackageHours: Int?,
    onPickPackageHours: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Пакеты времени", style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimePackageCard(
                modifier = Modifier.weight(1f),
                title = "3 часа",
                pricePerHour = "90 ₽/ч",
                selected = selectedPackageHours == 3,
                onClick = { onPickPackageHours(3) }
            )
            TimePackageCard(
                modifier = Modifier.weight(1f),
                title = "5 часов",
                pricePerHour = "80 ₽/ч",
                selected = selectedPackageHours == 5,
                onClick = { onPickPackageHours(5) }
            )
        }
    }
}

@Composable
private fun TimePackageCard(
    modifier: Modifier,
    title: String,
    pricePerHour: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                pricePerHour,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfiniteWindowTimeWheel(
    title: String,
    baseDate: LocalDate,
    currentAbsMin: Int,
    minLimitAbsMin: Int?,
    onCommitAbsMin: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = WHEEL_MID)
    val flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center)

    var programmatic by remember { mutableStateOf(false) }
    var pendingSnap by remember { mutableStateOf(false) }
    var snapping by remember { mutableStateOf(false) }

    val minLimitNow by rememberUpdatedState(minLimitAbsMin)
    fun minAlignedNow(): Int? = minLimitNow?.let { ceilToStep(it) }

    fun clampMin(x: Int): Int {
        val m = minAlignedNow() ?: return x
        return if (x < m) m else x
    }

    var baseAbs by remember { mutableStateOf(0) }
    fun absForIndex(index: Int): Int = baseAbs + (index - WHEEL_MID) * STEP

    suspend fun safeScrollTo(index: Int, animated: Boolean) {
        programmatic = true
        try {
            if (animated) listState.animateScrollToItem(index, 0) else listState.scrollToItem(index, 0)
        } finally {
            programmatic = false
        }
    }

    // стартовая синхронизация
    LaunchedEffect(Unit, minLimitAbsMin, currentAbsMin) {
        baseAbs = clampMin(roundToStep(currentAbsMin))
        safeScrollTo(WHEEL_MID, animated = false)
    }

    // синхронизация с VM, только когда колесо свободно
    LaunchedEffect(currentAbsMin, minLimitAbsMin) {
        if (!listState.isScrollInProgress && !programmatic) {
            baseAbs = clampMin(roundToStep(currentAbsMin))
            safeScrollTo(WHEEL_MID, animated = false)
        }
    }

    fun nearestIndexToCenter(): Int? {
        val layout = listState.layoutInfo
        val visible = layout.visibleItemsInfo
        if (visible.isEmpty()) return null

        val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
        return visible.minByOrNull { info ->
            val itemCenter = info.offset + info.size / 2
            abs(itemCenter - viewportCenter)
        }?.index
    }

    fun clampIndexByMinLimit(rawIdx: Int): Int {
        val m = minAlignedNow() ?: return rawIdx
        val minSteps = ceil((m - baseAbs).toDouble() / STEP.toDouble()).toInt()
        val minIdx = (WHEEL_MID + minSteps).coerceIn(0, WHEEL_COUNT - 1)
        return if (rawIdx < minIdx) minIdx else rawIdx
    }

    suspend fun snapCommitAndRecenter() {
        if (snapping) return
        snapping = true
        try {
            val idx0raw = nearestIndexToCenter() ?: return
            val idx0 = clampIndexByMinLimit(idx0raw)

            val raw = absForIndex(idx0)
            val picked = clampMin(roundToStep(raw))

            val steps = Math.floorDiv(picked - baseAbs, STEP)
            val targetIdx = (WHEEL_MID + steps).coerceIn(0, WHEEL_COUNT - 1)

            safeScrollTo(targetIdx, animated = true)

            onCommitAbsMin(picked)

            baseAbs = picked
            safeScrollTo(WHEEL_MID, animated = false)
        } finally {
            snapping = false
        }
    }

    val headerDate = remember(baseDate, baseAbs) {
        val dayOffset = Math.floorDiv(baseAbs, DAY_MIN)
        baseDate.plusDays(dayOffset.toLong()).format(DATE_TOP_FMT)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text(headerDate, style = MaterialTheme.typography.labelLarge)
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sidePadding = (maxWidth - TIME_ITEM_WIDTH) / 2

            Box(Modifier.fillMaxWidth()) {
                LazyRow(
                    state = listState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    horizontalArrangement = Arrangement.spacedBy(TIME_ITEM_SPACING)
                ) {
                    items(WHEEL_COUNT) { i ->
                        val absMin = absForIndex(i)
                        val m = minAlignedNow()
                        val enabled = m == null || absMin >= m

                        Box(Modifier.width(TIME_ITEM_WIDTH)) {
                            AssistChip(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(TIME_ITEM_HEIGHT),
                                enabled = enabled,
                                border = null,
                                onClick = {
                                    scope.launch {
                                        val picked = clampMin(roundToStep(absMin))
                                        val steps = Math.floorDiv(picked - baseAbs, STEP)
                                        val targetIdx = (WHEEL_MID + steps).coerceIn(0, WHEEL_COUNT - 1)

                                        safeScrollTo(targetIdx, animated = true)
                                        onCommitAbsMin(picked)

                                        baseAbs = picked
                                        safeScrollTo(WHEEL_MID, animated = false)
                                    }
                                },
                                label = {
                                    Text(
                                        text = formatTime(absMin),
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                // рамка выбора (центр)
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(TIME_ITEM_WIDTH)
                        .height(TIME_ITEM_HEIGHT)
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
        }
    }

    // магнит: срабатывает при отпускании, но если шла programmatic-анимация — откладываем
    LaunchedEffect(listState, minLimitAbsMin) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    if (programmatic) pendingSnap = true else snapCommitAndRecenter()
                }
            }
    }

    LaunchedEffect(programmatic) {
        if (!programmatic && pendingSnap && !listState.isScrollInProgress) {
            pendingSnap = false
            snapCommitAndRecenter()
        }
    }
}

@Composable
private fun DateStrip(
    selectedDate: LocalDate,
    onPick: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val tomorrow = remember(today) { today.plusDays(1) }

    // лента дат: начиная с today (прошлое в ленте не показываем)
    val start = remember(selectedDate, today) { today }
    val end = remember(selectedDate, today) {
        val base = if (selectedDate.year == today.year && selectedDate.month == today.month) today else selectedDate
        base.withDayOfMonth(base.lengthOfMonth())
    }

    val dates = remember(start, end) {
        generateSequence(start) { d -> d.plusDays(1).takeIf { it <= end } }.toList()
    }

    val selectedIndex = remember(selectedDate, dates) {
        dates.indexOfFirst { it == selectedDate }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedDate, dates) {
        val idx = dates.indexOfFirst { it == selectedDate }
        if (idx < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val first = visible.first().index
        val last = visible.last().index
        if (idx < first || idx > last) listState.animateScrollToItem(idx)
    }

    val rightLabel = remember(selectedDate, today, tomorrow) {
        when (selectedDate) {
            today -> "Сегодня"
            tomorrow -> "Завтра"
            else -> selectedDate.format(DATE_RIGHT_FMT)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Дата", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(rightLabel, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onOpenCalendar, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Календарь")
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            itemsIndexed(dates) { _, d ->
                val selected = d == selectedDate
                val dow = remember(d) { d.format(DATE_TILE_DOW_FMT).lowercase(RU).replace(".", "") }
                val day = d.dayOfMonth.toString()

                Surface(
                    onClick = { onPick(d) },
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(DATE_TILE_SIZE)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dow,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- вспомогательные функции ---

private fun formatTime(absMin: Int): String {
    val m = ((absMin % DAY_MIN) + DAY_MIN) % DAY_MIN
    return "%02d:%02d".format(m / 60, m % 60)
}

private fun formatDuration(min: Int): String {
    val d = Math.floorDiv(min, DAY_MIN)
    val rem = min - d * DAY_MIN
    val h = rem / 60
    val m = rem % 60
    return buildString {
        if (d > 0) append("${d}д ")
        if (h > 0) append("${h}ч ")
        append("${m}м")
    }
}

private fun roundToStep(x: Int): Int {
    val half = STEP / 2
    return if (x >= 0) ((x + half) / STEP) * STEP
    else -(((-x + half) / STEP) * STEP)
}

private fun ceilToStep(x: Int): Int {
    val r = x % STEP
    if (r == 0) return x
    return if (x > 0) x + (STEP - r) else x - r
}
