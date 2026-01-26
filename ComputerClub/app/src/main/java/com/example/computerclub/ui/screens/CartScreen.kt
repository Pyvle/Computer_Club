package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.vm.AppViewModel

@Composable
fun CartScreen(
    appVm: AppViewModel,
    onEditBooking: (String) -> Unit
) {
    var confirmHere by remember { mutableStateOf(false) }
    var payError by remember { mutableStateOf<String?>(null) }

    val clubName = remember(appVm.selectedClubId) {
        FakeData.clubs.firstOrNull { it.id == appVm.selectedClubId }?.name ?: "Клуб"
    }

    val bookingCost = appVm.cartBookingsTotal(appVm.selectedClubId)
    val productsCost = appVm.cartTotal(appVm.selectedClubId)
    val total = bookingCost + productsCost

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Корзина", style = MaterialTheme.typography.titleLarge)
                Text("Клуб: $clubName", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // --- БРОНИ ---
        item { Text("Бронирования", style = MaterialTheme.typography.titleMedium) }
        if (appVm.bookingCartLines.isEmpty()) {
            item { Text("Пока пусто. Добавь бронь выбрав места или через «Быстрая бронь».") }
        } else {
            items(appVm.bookingCartLines, key = { it.id }) { line ->
                val startDate = line.date.plusDays(line.startDayOffset.toLong())
                val endDate = line.date.plusDays(line.endDayOffset.toLong())
                val start = appVm.minToLabel(line.startMin)
                val end = appVm.minToLabel(line.endMin)

                val rate = appVm.bookingRateRubPerHour(line.packageHours)
                val cost = appVm.bookingLineCost(line)

                val startShort = "%02d.%02d".format(startDate.dayOfMonth, startDate.monthValue)
                val endShort = "%02d.%02d".format(endDate.dayOfMonth, endDate.monthValue)
                val seatsLabel = line.seatIds.joinToString(", ")

                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            line.packageHours?.let { pkg ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Пакет ${pkg}ч",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (startShort == endShort) "$startShort  $start — $end" else "$startShort $start — $endShort $end",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "Места: $seatsLabel • $rate ₽/ч",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$cost ₽",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { onEditBooking(line.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Изм.") }

                                TextButton(
                                    onClick = { appVm.removeBookingFromCart(line.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Удал.") }
                            }
                        }
                    }
                }
            }
        }

        item { Divider() }

        // --- ТОВАРЫ ---
        item { Text("Товары и услуги", style = MaterialTheme.typography.titleMedium) }
        if (appVm.cartLines.isEmpty()) {
            item { Text("Пока пусто.") }
        } else {
            items(appVm.cartLines, key = { it.productId + ":" + (it.variant ?: "") }) { line ->
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = line.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            line.variant?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${line.price} ₽ × ${line.qty}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${line.price * line.qty} ₽",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { appVm.changeQty(line.productId, line.variant, -1) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) { Text("-") }
                                TextButton(
                                    onClick = { appVm.changeQty(line.productId, line.variant, +1) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) { Text("+") }
                            }
                        }
                    }
                }
            }
        }

        item { Divider() }

        item {
            Text("Итого: $total ₽", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = confirmHere, onCheckedChange = { confirmHere = it })
                Text("Подтверждаю, что нахожусь в нужном клубе")
            }
        }

        item {
            payError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        item {
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
}
