package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

import kotlinx.coroutines.launch

@Composable
fun CartScreen(
    appVm: AppViewModel,
    onEditBooking: (String) -> Unit,
    onPaid: () -> Unit
) {
    val club = remember(appVm.selectedClubId, appVm.clubs) {
        appVm.clubs.firstOrNull { it.id == appVm.selectedClubId }
    }

    // не делаем forced-sync: иначе после переходов могут возвращаться удалённые товары
    LaunchedEffect(appVm.selectedClubId, appVm.user, club?.isBlocked) {
        if (appVm.user != null && club != null && !club.isBlocked) {
            appVm.syncCartProducts(force = false)
        }
    }

    if (appVm.user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Войдите, чтобы увидеть корзину")
        }
        return
    }

    if (club?.isBlocked == true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Вы заблокированы в этом клубе. Корзина недоступна.")
        }
        return
    }

    val clubName = club?.name ?: "Клуб"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val bookingCost = appVm.cartBookingsTotal(appVm.selectedClubId)
    val productsCost = appVm.cartTotal(appVm.selectedClubId)
    val total = bookingCost + productsCost

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // место под нижнюю кнопку "Оплатить"
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // --- Верх ---
            item(key = "top_club_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clubName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(onClick = { appVm.clearCart() }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Очистить корзину"
                        )
                    }
                }
            }

            // --- Брони ---
            item { Text("Бронирования", style = MaterialTheme.typography.titleMedium) }

            val hasBookings = appVm.bookingCartLines.isNotEmpty()
            if (!hasBookings) {
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
                                    text = if (startShort == endShort) {
                                        "$startShort  $start — $end"
                                    } else {
                                        "$startShort $start — $endShort $end"
                                    },
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
                                horizontalAlignment = Alignment.End,
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

                item(key = "booking_subtotal") {
                    Text(
                        text = "Итого бронирования: $bookingCost ₽",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Divider() }

            // --- Товары ---
            item { Text("Товары и услуги", style = MaterialTheme.typography.titleMedium) }

            val hasProducts = appVm.cartLines.isNotEmpty()
            if (!hasProducts) {
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
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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

                item(key = "products_subtotal") {
                    Text(
                        text = "Итого товары: $productsCost ₽",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Divider() }

            item(key = "total") {
                Text("Итого: $total ₽", style = MaterialTheme.typography.titleLarge)
            }
        }

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Button(
                onClick = {
                    appVm.checkoutServer(
                        onSuccess = { onPaid() },
                        onError = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
            ) {
                Text("Оплатить")
            }
        }
    }
}
