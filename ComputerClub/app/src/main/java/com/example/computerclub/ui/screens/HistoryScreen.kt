package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.computerclub.model.*
import com.example.computerclub.vm.AppViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun HistoryScreen(appVm: AppViewModel) {
    if (appVm.user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Войдите, чтобы увидеть историю заказов")
        }
        return
    }

    LaunchedEffect(Unit) { appVm.loadPurchaseHistory() }

    var tab by remember { mutableStateOf(0) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    // обновляем каждую минуту, чтобы статус брони менялся без перезахода на экран
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }

    val all = appVm.purchaseHistory

    // активное: есть будущая/активная бронь или заказ товаров не готов
    val active = remember(all.size, now) {
        all.filter { p ->
            val hasActiveBooking = p.bookingOrders.any { bookingStatus(now, it) != BookingStatus.DONE && bookingStatus(now, it) != BookingStatus.CANCELED }
            val productsActive = p.productOrder?.status == ProductOrderStatus.NOT_READY
            hasActiveBooking || productsActive
        }
    }

    val past = remember(all.size, now) {
        all.filterNot { active.contains(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Активные") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Прошедшие") })
        }

        if (appVm.historyLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val list = if (tab == 0) active else past

            if (list.isEmpty()) {
                Text(if (tab == 0) "Нет активных заказов/броней." else "Пока нет истории.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list, key = { it.id }) { purchase ->
                        PurchaseCard(purchase = purchase, now = now, appVm = appVm)
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseCard(purchase: Purchase, now: LocalDateTime, appVm: AppViewModel) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отменить заказ?") },
            text = { Text("Все бронирования будут отменены и места освобождены.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    appVm.cancelPurchase(purchase.id.toLong(), {}, {})
                }) { Text("Отменить заказ") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Назад") }
            }
        )
    }

    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(purchase.clubName, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDt(purchase.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (purchase.bookingOrders.isNotEmpty()) {
                Text("Бронирования", fontWeight = FontWeight.SemiBold)
                purchase.bookingOrders.forEach { b ->
                    val st = bookingStatus(now, b)
                    Text(
                        "• ${formatDt(b.startAt)} — ${formatDt(b.endAt)} • ${stLabel(st)} • места: ${b.seatLabels.joinToString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Итого бронирования: ${purchase.bookingTotalRub} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            purchase.productOrder?.let { po ->
                Divider()
                Text("Заказ", fontWeight = FontWeight.SemiBold)
                Text(
                    "Статус: ${orderStatusLabel(po.status)} • готовность: ${po.readyBy?.let { formatDt(it) } ?: "ASAP"} (${readyPolicyLabel(po.readyByPolicy)})",
                    style = MaterialTheme.typography.bodySmall
                )
                po.items.take(5).forEach { i ->
                    val v = i.variant?.let { " ($it)" } ?: ""
                    Text("• ${i.title}$v × ${i.qty}", style = MaterialTheme.typography.bodySmall)
                }
                if (po.items.size > 5) {
                    Text("…ещё ${po.items.size - 5}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Итого товары: ${purchase.productsTotalRub} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()
            Text("Итого: ${purchase.totalRub} ₽", style = MaterialTheme.typography.titleMedium)

            val canCancel = purchase.paymentStatus != "CANCELED" &&
                purchase.bookingOrders.any { bookingStatus(now, it) == BookingStatus.UPCOMING }

            if (purchase.paymentStatus == "CREATED") {
                Button(
                    onClick = { appVm.payPurchase(purchase.id.toLong(), {}, {}) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Оплатить")
                }
            }

            if (canCancel) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отменить бронь")
                }
            }
        }
    }
}

private fun bookingStatus(now: LocalDateTime, b: BookingOrder): BookingStatus {
    if (b.status == BookingStatus.CANCELED) return BookingStatus.CANCELED
    return when {
        now.isBefore(b.startAt) -> BookingStatus.UPCOMING
        now.isAfter(b.endAt) -> BookingStatus.DONE
        else -> BookingStatus.ACTIVE
    }
}

private fun stLabel(st: BookingStatus): String = when (st) {
    BookingStatus.UPCOMING -> "будущая"
    BookingStatus.ACTIVE -> "действует"
    BookingStatus.DONE -> "прошла"
    BookingStatus.CANCELED -> "отменена"
}

private fun orderStatusLabel(st: ProductOrderStatus): String = when (st) {
    ProductOrderStatus.NOT_READY -> "не готов"
    ProductOrderStatus.READY -> "готов"
    ProductOrderStatus.CANCELED -> "отменён"
}

private fun readyPolicyLabel(p: ReadyByPolicy): String = when (p) {
    ReadyByPolicy.ASAP -> "как можно скорее"
    ReadyByPolicy.BOOKING_START -> "к началу брони"
    ReadyByPolicy.CUSTOM -> "выбрано пользователем"
}

private val dtFmt = DateTimeFormatter.ofPattern("dd.MM HH:mm")
private fun formatDt(dt: LocalDateTime): String = dtFmt.format(dt)
