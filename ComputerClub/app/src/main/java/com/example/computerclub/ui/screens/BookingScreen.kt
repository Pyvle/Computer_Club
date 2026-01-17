package com.example.computerclub.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Seat
import com.example.computerclub.model.SeatAvailability
import com.example.computerclub.model.TimeRange
import com.example.computerclub.vm.AppViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookingScreen(appVm: AppViewModel) {
    val clubName = FakeData.clubs.firstOrNull { it.id == appVm.selectedClubId }?.name ?: "—"
    val seats: List<Seat> = FakeData.seatMapByClub[appVm.selectedClubId].orEmpty()

    var seatInfo by remember { mutableStateOf<String?>(null) }

    // Простые временные слоты (каждый час)
    val times = remember { (10..23).map { it * 60 } }

    val start = appVm.bookingDraft.startMin
    val end = appVm.bookingDraft.endMin
    val selectedRange = remember(start, end) {
        if (start != null && end != null && end > start) TimeRange(start, end) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Заголовок выбранного клуба + дата
        Text("Клуб: $clubName", style = MaterialTheme.typography.titleMedium)
        AssistChip(onClick = { /* заглушка выбора даты */ }, label = { Text(appVm.bookingDraft.dateLabel) })

        Text("Схема зала (заглушка)", style = MaterialTheme.typography.titleMedium)

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(seats) { seat ->
                val availability = appVm.seatAvailability(seat, selectedRange)
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
                                // Разрешаем выбирать место только если оно не полностью занято под выбранный интервал
                                if (availability != SeatAvailability.BOOKED) appVm.toggleSeat(seat.id)
                            },
                            onLongClick = { seatInfo = "${seat.label}\n${seat.equipment}" }
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

        // Выбор времени
        Text("Время", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TimeDropDown(
                    label = "Начало",
                    times = times,
                    selected = start,
                    onSelect = { appVm.setTime(it, end) },
                    toLabel = appVm::minToLabel
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                TimeDropDown(
                    label = "Конец",
                    times = times.filter { t -> start == null || t > start },
                    selected = end,
                    onSelect = { appVm.setTime(start, it) },
                    toLabel = appVm::minToLabel
                )
            }
        }

        Button(
            onClick = { /* переход в корзину делается через нижнюю панель */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = (selectedRange != null && appVm.bookingDraft.selectedSeatIds.isNotEmpty())
        ) {
            Text("Бронировать → в корзину")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropDown(
    label: String,
    times: List<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    toLabel: (Int) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    val value = selected?.let(toLabel) ?: "—"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = { onSelect(null); expanded = false }
            )
            times.forEach { t ->
                DropdownMenuItem(
                    text = { Text(toLabel(t)) },
                    onClick = { onSelect(t); expanded = false }
                )
            }
        }
    }
}
