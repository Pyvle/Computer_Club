package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.model.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.computerclub.ui.components.*
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurface
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.TextSecondary
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(appVm: AppViewModel) {
    if (appVm.user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppEmptyState(
                icon = Icons.Outlined.History,
                title = "Войдите в аккаунт",
                subtitle = "Чтобы увидеть историю заказов"
            )
        }
        return
    }

    LaunchedEffect(Unit) { appVm.loadPurchaseHistory() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    val active = remember(all.size, now) {
        all.filter { p ->
            if (p.bookingOrders.isNotEmpty()) {
                p.bookingOrders.any {
                    val st = resolveBookingStatus(now, it)
                    st == BookingStatus.UPCOMING || st == BookingStatus.ACTIVE
                }
            } else {
                p.productOrder?.status == ProductOrderStatus.NOT_READY
            }
        }
    }

    val past = remember(all.size, now) { all.filterNot { active.contains(it) } }

    Box(Modifier.fillMaxSize()) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = AppSurface,
            contentColor = BrandIndigo,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                    color = BrandIndigo
                )
            }
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Активные") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Прошедшие") })
        }

        if (appVm.historyLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
        } else {
            val list = if (tab == 0) active else past

            if (list.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Outlined.History,
                    title = if (tab == 0) "Нет активных заказов" else "История пуста",
                    subtitle = if (tab == 0) "Забронируйте место или закажите товары" else "Завершённые заказы появятся здесь"
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list, key = { it.id }) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            now = now,
                            appVm = appVm,
                            onMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                }
            }
        }
    } // Column
    } // Box
}

@Composable
private fun PurchaseCard(
    purchase: Purchase,
    now: LocalDateTime,
    appVm: AppViewModel,
    onMessage: (String) -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отменить заказ?") },
            text = { Text("Все бронирования будут отменены и места освобождены.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    appVm.cancelPurchase(
                        purchase.id.toLong(),
                        onSuccess = { onMessage("Заказ отменён") },
                        onError = { onMessage("Не удалось отменить заказ") }
                    )
                }) { Text("Отменить заказ", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Назад") }
            }
        )
    }

    AppCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purchase.clubName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppStatusChip(
                        label = purchase.paymentStatus.toPaymentLabel(),
                        tone = purchase.paymentStatus.toPaymentChipTone()
                    )
                    Text(
                        text = formatDt(purchase.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (purchase.bookingOrders.isNotEmpty()) {
                Text(
                    text = "Бронирования",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                purchase.bookingOrders.forEach { b ->
                    val st = resolveBookingStatus(now, b)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${formatDt(b.startAt)} — ${formatDt(b.endAt)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Места: ${b.seatLabels.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        AppStatusChip(label = st.toLabel(), tone = st.toChipTone())
                    }
                }
                Text(
                    text = "Итого бронирования: ${purchase.bookingTotalRub} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            purchase.productOrder?.let { po ->
                HorizontalDivider(color = AppBorder)
                Text(
                    text = "Заказ",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                po.items.take(5).forEach { i ->
                    val v = i.variant?.let { " ($it)" } ?: ""
                    Text(
                        text = "• ${i.title}$v × ${i.qty}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (po.items.size > 5) {
                    Text(
                        text = "…ещё ${po.items.size - 5}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "Итого товары: ${purchase.productsTotalRub} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = AppBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Итого: ${purchase.totalRub} ₽",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (purchase.paymentStatus == "CREATED") {
                AppPrimaryButton(
                    text = "Оплатить",
                    onClick = {
                        appVm.payPurchase(
                            purchase.id.toLong(),
                            onSuccess = { onMessage("Заказ оплачен") },
                            onError = { onMessage("Не удалось оплатить заказ") }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val canCancel = purchase.paymentStatus != "CANCELED" &&
                purchase.bookingOrders.any { resolveBookingStatus(now, it) == BookingStatus.UPCOMING }

            if (canCancel) {
                AppSecondaryButton(
                    text = "Отменить бронь",
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun resolveBookingStatus(now: LocalDateTime, b: BookingOrder): BookingStatus {
    if (b.status == BookingStatus.CANCELED) return BookingStatus.CANCELED
    return when {
        now.isBefore(b.startAt) -> BookingStatus.UPCOMING
        now.isAfter(b.endAt) -> BookingStatus.DONE
        else -> BookingStatus.ACTIVE
    }
}

private val dtFmt = DateTimeFormatter.ofPattern("dd.MM HH:mm")
private fun formatDt(dt: LocalDateTime): String = dtFmt.format(dt)
