package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.computerclub.model.Product
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSearchScreen(appVm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.trim() }

    var needConfirmClub by remember { mutableStateOf(false) }
    var sheetItem by remember { mutableStateOf<ShopItem?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val club = remember(appVm.selectedClubId, appVm.clubs) {
        appVm.clubs.firstOrNull { it.id == appVm.selectedClubId }
    }

    if (appVm.user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Войдите, чтобы искать товары")
        }
        return
    }

    if (club?.isBlocked == true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Вы заблокированы в этом клубе. Выберите другой клуб.")
        }
        return
    }

    // не делаем forced-sync при каждом заходе на экран
    LaunchedEffect(appVm.selectedClubId, appVm.user, club?.isBlocked) {
        if (appVm.user != null && club != null && !club.isBlocked) {
            appVm.loadShopData(force = false)
            appVm.syncCartProducts(force = false)
        }
    }

    // все товары меню для выбранного клуба как плоский список
    val allItems = remember(appVm.selectedClubId, appVm.shopProducts) {
        appVm.shopProducts.flatMap { it.toShopItems() }
    }

    val filtered = remember(normalizedQuery, allItems) {
        if (normalizedQuery.isBlank()) allItems
        else allItems.filter { it.matches(normalizedQuery) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "club_card") {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(club?.name ?: "Клуб", style = MaterialTheme.typography.titleMedium)
                            Text(club?.address ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { club?.let { appVm.toggleFavoriteClub(it.id) } }) {
                            Icon(
                                imageVector = if (club != null && appVm.isFavoriteClub(club.id))
                                    Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Избранное"
                            )
                        }
                    }
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск по товарам и услугам") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Ничего не найдено") }
                }
            } else {
                val rows = filtered.chunked(2)
                rows.forEachIndexed { rowIndex, rowItems ->
                    item(key = "row_$rowIndex") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProductTile(
                                modifier = Modifier.weight(1f),
                                item = rowItems[0],
                                appVm = appVm,
                                onOpenDetails = {
                                    if (!appVm.clubConfirmed) needConfirmClub = true
                                    sheetItem = rowItems[0]
                                },
                                onPlusFirst = { itx ->
                                    if (!appVm.clubConfirmed) needConfirmClub = true
                                    appVm.addProduct(itx.product, itx.variant)
                                },
                                onMinus = { itx -> appVm.changeQty(itx.product.id, itx.variant, -1) },
                                onPlus = { itx -> appVm.changeQty(itx.product.id, itx.variant, +1) }
                            )

                            if (rowItems.size > 1) {
                                ProductTile(
                                    modifier = Modifier.weight(1f),
                                    item = rowItems[1],
                                    appVm = appVm,
                                    onOpenDetails = {
                                        if (!appVm.clubConfirmed) needConfirmClub = true
                                        sheetItem = rowItems[1]
                                    },
                                    onPlusFirst = { itx ->
                                        if (!appVm.clubConfirmed) needConfirmClub = true
                                        appVm.addProduct(itx.product, itx.variant)
                                    },
                                    onMinus = { itx -> appVm.changeQty(itx.product.id, itx.variant, -1) },
                                    onPlus = { itx -> appVm.changeQty(itx.product.id, itx.variant, +1) }
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(120.dp)) }
        }

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { /* переход в корзину через нижнюю панель */ },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("В корзину") }
            }
        }
    }

    if (needConfirmClub) {
        AlertDialog(
            onDismissRequest = { needConfirmClub = false },
            confirmButton = {
                TextButton(onClick = {
                    appVm.confirmClub()
                    needConfirmClub = false
                }) { Text("Подтвердить") }
            },
            dismissButton = { TextButton(onClick = { needConfirmClub = false }) { Text("Отмена") } },
            title = { Text("Подтверди клуб") },
            text = { Text("Ты сейчас заказываешь для клуба: ${club?.name ?: "Клуб"}") }
        )
    }

    sheetItem?.let { itx ->
        val p = itx.product
        val sheetState = rememberModalBottomSheetState()

        val qtyInCart by remember(appVm.cartLines, p.id, itx.variant) {
            derivedStateOf {
                appVm.cartLines.firstOrNull { it.productId == p.id && it.variant == itx.variant }?.qty ?: 0
            }
        }

        ModalBottomSheet(
            onDismissRequest = { sheetItem = null },
            sheetState = sheetState
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { sheetItem = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Text("Фото товара") }

                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (itx.variant != null) {
                    AssistChip(onClick = { }, label = { Text(itx.variant) })
                }
                Text(text = p.description, style = MaterialTheme.typography.bodyMedium)

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${p.price} ₽",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        QtyPill(
                            qty = qtyInCart,
                            onMinus = { appVm.changeQty(p.id, itx.variant, -1) },
                            onPlus = { appVm.changeQty(p.id, itx.variant, +1) },
                            onAddFirst = { appVm.addProduct(p, itx.variant) }
                        )
                    }
                }
            }
        }
    }
}

private data class ShopItem(
    val product: Product,
    val variant: String?
)

private fun Product.toShopItems(): List<ShopItem> {
    return if (variants.isEmpty()) listOf(ShopItem(this, null))
    else variants.map { v -> ShopItem(this, v) }
}

private fun ShopItem.matches(q: String): Boolean {
    val qq = q.trim()
    if (qq.isBlank()) return true
    return product.title.contains(qq, ignoreCase = true) ||
            product.description.contains(qq, ignoreCase = true) ||
            (variant?.contains(qq, ignoreCase = true) == true)
}

@Composable
private fun ProductTile(
    modifier: Modifier,
    item: ShopItem,
    appVm: AppViewModel,
    onOpenDetails: () -> Unit,
    onPlusFirst: (ShopItem) -> Unit,
    onMinus: (ShopItem) -> Unit,
    onPlus: (ShopItem) -> Unit
) {
    val product = item.product
    val qty by remember(appVm.cartLines, product.id, item.variant) {
        derivedStateOf {
            appVm.cartLines.firstOrNull { it.productId == product.id && it.variant == item.variant }?.qty ?: 0
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .clickable { onOpenDetails() },
                contentAlignment = Alignment.Center
            ) {
                Text("Фото", style = MaterialTheme.typography.labelLarge)
            }

            Text(
                text = "${product.price} ₽",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            Text(
                text = product.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val sub = remember(item) {
                when {
                    item.variant != null && product.description.isNotBlank() -> "${item.variant} · ${product.description}"
                    item.variant != null -> item.variant
                    else -> product.description
                }
            }
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            QtyBar(
                qty = qty,
                onAddFirst = { onPlusFirst(item) },
                onMinus = { onMinus(item) },
                onPlus = { onPlus(item) }
            )
        }
    }
}

@Composable
private fun QtyBar(
    qty: Int,
    onAddFirst: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (qty <= 0) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            if (qty <= 0) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.clickable { onAddFirst() }
                )
            } else {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.clickable { onMinus() }
                )
                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.clickable { onPlus() }
                )
            }
        }
    }
}

@Composable
private fun QtyPill(
    qty: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onAddFirst: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (qty <= 0) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onAddFirst() }
                )
            } else {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onMinus() }
                )
                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onPlus() }
                )
            }
        }
    }
}
