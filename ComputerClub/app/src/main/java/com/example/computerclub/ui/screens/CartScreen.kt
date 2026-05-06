package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.computerclub.ui.components.AppCard
import com.example.computerclub.ui.components.AppEmptyState
import com.example.computerclub.ui.components.AppPrimaryButton
import com.example.computerclub.ui.components.AppSecondaryButton
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurfaceAlt
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.BrandIndigoSoft
import com.example.computerclub.ui.theme.TextSecondary
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun CartScreen(
    appVm: AppViewModel,
    onEditBooking: (String) -> Unit,
    onPaid: () -> Unit,
    onLoginRequired: () -> Unit
) {
    val club = remember(appVm.selectedClubId, appVm.clubs) {
        appVm.clubs.firstOrNull { it.id == appVm.selectedClubId }
    }

    LaunchedEffect(appVm.selectedClubId, appVm.user, club?.isBlocked) {
        if (appVm.user != null && club != null && !club.isBlocked) {
            appVm.syncCartProducts(force = false)
        }
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

    val hasAnything = appVm.bookingCartLines.isNotEmpty() || appVm.cartLines.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // --- Шапка ---
            item(key = "top_club_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clubName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasAnything) {
                        IconButton(onClick = {
                            appVm.clearCart(onError = {
                                scope.launch { snackbarHostState.showSnackbar("Не удалось очистить корзину") }
                            })
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Очистить корзину",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (!hasAnything) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.7f),
                        contentAlignment = Alignment.Center
                    ) {
                        AppEmptyState(
                            icon = Icons.Outlined.ShoppingCart,
                            title = "Корзина пуста",
                            subtitle = "Добавьте бронь или товары из магазина"
                        )
                    }
                }
                return@LazyColumn
            }

            // --- Бронирования ---
            item { Text("Бронирования", style = MaterialTheme.typography.titleMedium) }

            val hasBookings = appVm.bookingCartLines.isNotEmpty()
            if (!hasBookings) {
                item {
                    Text(
                        text = "Добавьте бронь через «Быстрая бронь» или выбор мест.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
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

                    AppCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                line.packageHours?.let { pkg ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = BrandIndigoSoft
                                    ) {
                                        Text(
                                            text = "Пакет ${pkg}ч",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandIndigo,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (startShort == endShort) "$startShort  $start — $end"
                                           else "$startShort $start — $endShort $end",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Места: $seatsLabel • $rate ₽/ч",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "$cost ₽", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { onEditBooking(line.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Изм.", style = MaterialTheme.typography.labelMedium, color = BrandIndigo)
                                    }
                                    TextButton(
                                        onClick = { appVm.removeBookingFromCart(line.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Удал.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "booking_subtotal") {
                    Text(
                        text = "Итого бронирования: $bookingCost ₽",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            item { HorizontalDivider(color = AppBorder) }

            // --- Товары ---
            item { Text("Товары и услуги", style = MaterialTheme.typography.titleMedium) }

            val hasProducts = appVm.cartLines.isNotEmpty()
            if (!hasProducts) {
                item {
                    Text(
                        text = "Добавьте товары из магазина.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                items(appVm.cartLines, key = { it.productId + ":" + (it.variant ?: "") }) { line ->
                    AppCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${line.price} ₽ × ${line.qty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
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
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { appVm.changeQty(line.productId, line.variant, -1) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) { Text("−", style = MaterialTheme.typography.titleMedium, color = TextSecondary) }
                                    TextButton(
                                        onClick = { appVm.changeQty(line.productId, line.variant, +1) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) { Text("+", style = MaterialTheme.typography.titleMedium, color = BrandIndigo) }
                                }
                            }
                        }
                    }
                }

                item(key = "products_subtotal") {
                    Text(
                        text = "Итого товары: $productsCost ₽",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            item { HorizontalDivider(color = AppBorder) }

            // --- Итого ---
            item(key = "total") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Итого", style = MaterialTheme.typography.titleLarge)
                    Text("$total ₽", style = MaterialTheme.typography.titleLarge, color = BrandIndigo)
                }
            }
        }

        // нижняя кнопка
        Surface(
            color = AppSurfaceAlt,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                AppPrimaryButton(
                    text = if (appVm.user == null) "Войдите для оплаты" else "Оплатить $total ₽",
                    onClick = {
                        if (appVm.user == null) {
                            onLoginRequired()
                        } else {
                            appVm.checkoutServer(
                                onSuccess = { onPaid() },
                                onError = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    },
                    enabled = hasAnything,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
