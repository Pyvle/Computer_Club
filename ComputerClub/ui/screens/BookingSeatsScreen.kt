package com.example.computerclub.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Seat
import com.example.computerclub.model.SeatAvailability
import com.example.computerclub.model.TimeRange
import com.example.computerclub.vm.AppViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookingSeatsScreen(
    appVm: AppViewModel,
    onGoToCart: () -> Unit
) {
    val clubId = appVm.selectedClubId
    val club = FakeData.clubs.firstOrNull { it.id == clubId }
    val seats: List<Seat> = FakeData.seatMapByClub[clubId].orEmpty()

    if (club == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Сначала выбери клуб.")
        }
        return
    }

    val isFav = appVm.isFavoriteClub(club.id)

    val date = appVm.bookingDraft.date
    val start = appVm.bookingDraft.startMin
    val end = appVm.bookingDraft.endMin
    val range = TimeRange(start, end)

    var seatInfo by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Шапка внутри экрана: клуб + избранное
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(club.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${formatDateShort(date)} • ${formatTime(start)}–${formatTime(end)} • ${formatDuration(end - start)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = { appVm.toggleFavoriteClub(club.id) }) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Избранное"
                )
            }
        }

        Text("Выбор мест", style = MaterialTheme.typography.titleMedium)

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(seats, key = { it.id }) { seat ->
                val availability = appVm.seatAvailability(seat, range)
                val selected = appVm.bookingDraft.selectedSeatIds.contains(seat.id)

                val containerColor = when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    availability == SeatAvailability.BOOKED -> MaterialTheme.colorScheme.surfaceVariant
                    availability == SeatAvailability.PARTIAL -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .combinedClickable(
                            onClick = {
                                if (availability != SeatAvailability.BOOKED) {
                                    appVm.toggleSeat(seat.id)
                                }
                            },
                            onLongClick = {
                                seatInfo = "${seat.label}\n${seat.equipment}"
                            }
                        ),
                    colors = CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(seat.label, style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (seat.type.name == "VIP") "VIP" else "Обычн.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Button(
            onClick = onGoToCart,
            modifier = Modifier.fillMaxWidth(),
            enabled = appVm.bookingDraft.selectedSeatIds.isNotEmpty()
        ) {
            Text("В корзину")
        }
    }

    if (seatInfo != null) {
        AlertDialog(
            onDismissRequest = { seatInfo = null },
            confirmButton = {
                TextButton(onClick = { seatInfo = null }) { Text("Ок") }
            },
            title = { Text("Информация о месте") },
            text = { Text(seatInfo!!) }
        )
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

private fun formatDateShort(d: java.time.LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    return d.format(fmt)
}
