package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.vm.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow

@Composable
fun BookingSetupScreen(
    appVm: AppViewModel,
    onNext: () -> Unit
) {
    val clubId = appVm.selectedClubId
    val club = FakeData.clubs.firstOrNull { it.id == clubId }

    if (club == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Сначала выбери клуб.")
        }
        return
    }

    val isFav = appVm.isFavoriteClub(club.id)

    // даты: сегодня + 14 дней
    val dates = remember { (0..14).map { LocalDate.now().plusDays(it.toLong()) } }

    // лента времени: каждые 30 минут
    val baseTimes = remember { (0 until 24 * 60 step 30).toList() }

    val date = appVm.bookingDraft.date
    val start = appVm.bookingDraft.startMin
    val end = appVm.bookingDraft.endMin

    val durationText = remember(start, end) { formatDuration(end - start) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Клуб + избранное
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(club.name, style = MaterialTheme.typography.titleLarge)
                Text(club.location, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { appVm.toggleFavoriteClub(club.id) }) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Избранное"
                )
            }
        }

        // Дата
        Text("Дата", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(dates) { d ->
                val selected = d == date
                FilterChip(
                    selected = selected,
                    onClick = { appVm.setBookingDate(d) },
                    label = { Text(formatDate(d)) }
                )
            }
        }

        // Время: две "ленты" + длительность по центру
        Text("Время", style = MaterialTheme.typography.titleMedium)

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Начало", style = MaterialTheme.typography.labelLarge)
                TimeWheel(
                    baseTimes = baseTimes,
                    selectedMin = start,
                    toLabel = ::formatTime,
                    onSelected = { appVm.setStartMin(it) }
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Длительность", style = MaterialTheme.typography.labelLarge)
                Text(durationText, style = MaterialTheme.typography.titleMedium)
            }

            Column(Modifier.weight(1f)) {
                Text("Конец", style = MaterialTheme.typography.labelLarge)
                TimeWheel(
                    baseTimes = baseTimes,
                    selectedMin = end,
                    toLabel = ::formatTime,
                    onSelected = { appVm.setEndMin(it) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее → выбор мест")
        }
    }
}

@Composable
private fun TimeWheel(
    baseTimes: List<Int>,
    selectedMin: Int,
    toLabel: (Int) -> String,
    onSelected: (Int) -> Unit
) {
    val baseSize = baseTimes.size
    val bigCount = 10_000

    // Ставим "середину", чтобы можно было крутить "бесконечно"
    val initialIndex = remember(selectedMin) {
        val baseIndex = baseTimes.indexOf(selectedMin).let { if (it >= 0) it else 0 }
        val mid = bigCount / 2
        (mid - (mid % baseSize)) + baseIndex
    }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Берём элемент, который ближе всего к центру viewport — считаем его выбранным
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo }
            .collectLatest { visible ->
                if (visible.isEmpty()) return@collectLatest

                val viewportStart = state.layoutInfo.viewportStartOffset
                val viewportEnd = state.layoutInfo.viewportEndOffset
                val center = (viewportStart + viewportEnd) / 2

                val closest = visible.minByOrNull { info ->
                    abs((info.offset + info.size / 2) - center)
                } ?: return@collectLatest

                val value = baseTimes[closest.index % baseSize]
                if (value != selectedMin) onSelected(value)
            }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        // “Окошко” по центру
        Box(
            Modifier
                .align(Alignment.Center)
                .width(96.dp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
        )

        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(bigCount) { i ->
                val v = baseTimes[i % baseSize]
                val selected = v == selectedMin

                Text(
                    text = toLabel(v),
                    style = if (selected) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            }
        }
    }
}

private fun formatTime(min: Int): String {
    val h = (min / 60) % 24
    val m = min % 60
    return "%02d:%02d".format(h, m)
}

private fun formatDuration(deltaMin: Int): String {
    val d = deltaMin.coerceAtLeast(0)
    val h = d / 60
    val m = d % 60
    return if (h > 0) "${h}ч ${m}м" else "${m}м"
}

private fun formatDate(d: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("d MMMM")
    return d.format(fmt)
}
