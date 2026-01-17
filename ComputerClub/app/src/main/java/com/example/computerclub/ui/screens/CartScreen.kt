package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.vm.AppViewModel

@Composable
fun CartScreen(appVm: AppViewModel) {
    val clubName = FakeData.clubs.first { it.id == appVm.selectedClubId }.name
    var confirmHere by remember { mutableStateOf(false) }
    var payError by remember { mutableStateOf<String?>(null) }

    val booking = appVm.bookingDraft
    val bookingCost = remember(booking.startMin, booking.endMin, booking.selectedSeatIds) {
        // мок-расчёт: 200₽/час за обычное место, 350₽/час за VIP
        val start = booking.startMin
        val end = booking.endMin
        if (start == null || end == null || end <= start) 0 else {
            val hours = (end - start) / 60
            val seats = FakeData.seatMapByClub[appVm.selectedClubId].orEmpty()
            val selected = seats.filter { booking.selectedSeatIds.contains(it.id) }
            selected.sumOf { seat ->
                val rate = if (seat.type.name == "VIP") 350 else 200
                rate * hours
            }
        }
    }

    val productsCost = appVm.cartTotal()
    val total = bookingCost + productsCost

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Клуб: $clubName", style = MaterialTheme.typography.titleMedium)

        if (booking.selectedSeatIds.isNotEmpty()) {
            val start = booking.startMin?.let(appVm::minToLabel) ?: "—"
            val end = booking.endMin?.let(appVm::minToLabel) ?: "—"
            Text("Бронирование: $start — $end")
            Text("Места: ${booking.selectedSeatIds.joinToString()}")
            Text("Стоимость брони (мок): $bookingCost ₽")
        } else {
            Text("Бронирование не выбрано.")
        }

        Divider()

        Text("Товары и услуги", style = MaterialTheme.typography.titleMedium)
        if (appVm.cartLines.isEmpty()) {
            Text("Пока пусто.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(appVm.cartLines) { line ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(line.title)
                            line.variant?.let { Text("Вариант: $it") }
                            Text("${line.price} ₽ × ${line.qty} = ${line.price * line.qty} ₽")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { appVm.changeQty(line.productId, line.variant, -1) }) { Text("-") }
                                OutlinedButton(onClick = { appVm.changeQty(line.productId, line.variant, +1) }) { Text("+") }
                            }
                        }
                    }
                }
            }
        }

        Divider()
        Text("Итого: $total ₽", style = MaterialTheme.typography.titleLarge)

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = confirmHere, onCheckedChange = { confirmHere = it })
            Text("Подтверждаю, что нахожусь в нужном клубе")
        }

        payError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (!confirmHere) { payError = "Подтверди клуб"; return@Button }
                    val ok = appVm.checkoutByWallet(total)
                    payError = if (ok) null else "Недостаточно средств / не выполнен вход"
                },
                modifier = Modifier.weight(1f)
            ) { Text("Оплатить кошельком") }

            OutlinedButton(
                onClick = {
                    if (!confirmHere) { payError = "Подтверди клуб"; return@OutlinedButton }
                    val ok = appVm.checkoutByCard(total)
                    payError = if (ok) null else "Ошибка оплаты картой (мок)"
                },
                modifier = Modifier.weight(1f)
            ) { Text("Картой") }
        }
    }
}
