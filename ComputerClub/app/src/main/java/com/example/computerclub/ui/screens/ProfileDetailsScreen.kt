package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.computerclub.app.Routes
import com.example.computerclub.model.Purchase
import com.example.computerclub.ui.components.AppCard
import com.example.computerclub.ui.components.AppEmptyState
import com.example.computerclub.ui.components.AppScreenContainer
import com.example.computerclub.ui.components.AppSecondaryButton
import com.example.computerclub.ui.components.AppStatusChip
import com.example.computerclub.ui.components.toPaymentChipTone
import com.example.computerclub.ui.components.toPaymentLabel
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurfaceAlt
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.BrandIndigoDeep
import com.example.computerclub.ui.theme.BrandIndigoSoft
import com.example.computerclub.ui.theme.StatusError
import com.example.computerclub.ui.theme.TextMuted
import com.example.computerclub.ui.theme.TextSecondary
import com.example.computerclub.vm.AppViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ProfileDetailsScreen(appVm: AppViewModel, nav: NavHostController) {
    val user = appVm.user

    LaunchedEffect(user?.id) {
        if (user != null) {
            if (appVm.purchaseHistory.isEmpty()) appVm.loadPurchaseHistory()
            if (appVm.favoriteClubIds.isEmpty()) appVm.loadFavorites()
            if (appVm.clubs.isEmpty()) appVm.loadClubs()
        }
    }

    AppScreenContainer {
        if (user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Не авторизован.", color = TextSecondary)
            }
            return@AppScreenContainer
        }

        val purchases = appVm.purchaseHistory
        val pendingPurchases = purchases.filter { it.paymentStatus == "CREATED" }
        val recentPurchases = purchases.filter { it.paymentStatus != "CREATED" }.take(4)
        val favoriteClubs = appVm.clubs.filter { it.id in appVm.favoriteClubIds }
        val totalSpentRub = purchases.filter { it.paymentStatus == "PAID" }.sumOf { it.totalRub }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(BrandIndigoSoft, Color.White)
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = user.phone,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Клиент приложения",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            StatCell("Заказов", purchases.size.toString(), Modifier.weight(1f))
                            StatCell("Потрачено", "${formatRub(totalSpentRub)} ₽", Modifier.weight(1f))
                            StatCell("Избранных", favoriteClubs.size.toString(), Modifier.weight(1f))
                        }

                        AppSecondaryButton(
                            text = "Выбрать клуб",
                            onClick = { nav.navigate(Routes.Clubs) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (pendingPurchases.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Ожидают оплаты",
                        count = pendingPurchases.size.toString(),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = BrandIndigo
                            )
                        }
                    )
                }

                items(pendingPurchases, key = { it.id }) { purchase ->
                    PurchaseSummaryCard(
                        purchase = purchase,
                        onOpenHistory = { nav.navigate(Routes.History) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Последние заказы",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    action = if (purchases.isNotEmpty()) "Вся история" else null,
                    onAction = if (purchases.isNotEmpty()) ({ nav.navigate(Routes.History) }) else null
                )
            }

            if (recentPurchases.isEmpty()) {
                item {
                    AppCard {
                        AppEmptyState(
                            icon = Icons.Outlined.History,
                            title = "Заказов пока нет",
                            subtitle = "После первой брони или покупки они появятся здесь",
                            action = "Выбрать клуб",
                            onActionClick = { nav.navigate(Routes.Clubs) }
                        )
                    }
                }
            } else {
                items(recentPurchases, key = { it.id }) { purchase ->
                    PurchaseSummaryCard(
                        purchase = purchase,
                        onOpenHistory = { nav.navigate(Routes.History) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Избранные клубы",
                    count = favoriteClubs.size.takeIf { it > 0 }?.toString(),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = StatusError
                        )
                    },
                    action = "Все клубы",
                    onAction = { nav.navigate(Routes.Clubs) }
                )
            }

            if (favoriteClubs.isEmpty()) {
                item {
                    AppCard {
                        AppEmptyState(
                            icon = Icons.Outlined.BookmarkBorder,
                            title = "Избранных клубов пока нет",
                            subtitle = "Добавляйте клубы в избранное, чтобы быстро к ним возвращаться",
                            action = "Перейти к клубам",
                            onActionClick = { nav.navigate(Routes.Clubs) }
                        )
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteClubs, key = { it.id }) { club ->
                            FavoriteClubCard(
                                name = club.name,
                                address = club.address,
                                imageUrl = club.imageUrl,
                                onOpen = { nav.navigate("club_details/${club.id}") },
                                onBook = {
                                    appVm.chooseClub(club.id)
                                    nav.navigate(Routes.Booking) { launchSingleTop = true }
                                },
                                onRemove = { appVm.toggleFavoriteClub(club.id) }
                            )
                        }
                    }
                }
            }

            item {
                AppCard(onClick = { nav.navigate(Routes.About) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                            Text(
                                text = "О приложении",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
            }

            item {
                AppSecondaryButton(
                    text = "Выйти из аккаунта",
                    onClick = { appVm.logout() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: @Composable () -> Unit,
    count: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (count != null) {
                Box(
                    modifier = Modifier
                        .background(AppSurfaceAlt, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = count,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = BrandIndigo,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
private fun PurchaseSummaryCard(
    purchase: Purchase,
    onOpenHistory: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    AppCard(onClick = onOpenHistory) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = purchase.clubName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatter.format(purchase.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${purchase.totalRub} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    AppStatusChip(
                        label = purchase.paymentStatus.toPaymentLabel(),
                        tone = purchase.paymentStatus.toPaymentChipTone()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (purchase.bookingOrders.isNotEmpty()) {
                    SmallPill("Бронь: ${purchase.bookingOrders.size}")
                }
                if (purchase.productOrder != null) {
                    SmallPill("Товары")
                }
            }
        }
    }
}

@Composable
private fun SmallPill(text: String) {
    Box(
        modifier = Modifier
            .background(BrandIndigoSoft, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = BrandIndigoDeep
        )
    }
}

@Composable
private fun FavoriteClubCard(
    name: String,
    address: String,
    imageUrl: String?,
    onOpen: () -> Unit,
    onBook: () -> Unit,
    onRemove: () -> Unit
) {
    AppCard(
        modifier = Modifier.width(252.dp),
        onClick = onOpen
    ) {
        Column {
            Box {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .background(BrandIndigoSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.White.copy(alpha = 0.92f), shape = CircleShape)
                        .clickable(onClick = onRemove)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(color = AppBorder)

                AppSecondaryButton(
                    text = "К бронированию",
                    onClick = onBook,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatRub(value: Int): String = "%,d".format(value).replace(',', ' ')
