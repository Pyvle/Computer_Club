package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Product
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(appVm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(FakeData.categories.first().id) }

    var needConfirmClub by remember { mutableStateOf(false) }
    var sheetProduct by remember { mutableStateOf<Product?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val club = remember(appVm.selectedClubId) {
        FakeData.clubs.first { it.id == appVm.selectedClubId }
    }

    // Категории НЕ скрываем. Фильтруем только товары внутри категорий.
    val sectionsAll = remember(query) {
        FakeData.categories.map { cat ->
            val list = FakeData.products
                .filter { it.categoryId == cat.id }
                .filter {
                    query.isBlank() ||
                            it.title.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            cat to list
        }
    }

    // Хедеры сверху в LazyColumn: клуб, поиск, чипы — всегда 3
    val headerOffset = 3

    // Индексы заголовков категорий для прыжка по чипу
    // Даже если товаров 0 — оставляем минимум 1 item под заголовком, чтобы индексы не "ехали".
    val headerIndexByCatId = remember(sectionsAll) {
        val map = mutableMapOf<String, Int>()
        var idx = headerOffset
        sectionsAll.forEach { (cat, list) ->
            val rows = maxOf((list.size + 1) / 2, 1)
            map[cat.id] = idx
            idx += 1 + rows
        }
        map
    }

    // Подсветка категории при скролле (по всем категориям)
    LaunchedEffect(listState, headerIndexByCatId) {
        val headers = FakeData.categories.mapNotNull { cat ->
            headerIndexByCatId[cat.id]?.let { cat.id to it }
        }.sortedBy { it.second }

        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIdx ->
                val current = headers.lastOrNull { it.second <= firstIdx }?.first
                if (current != null && current != selectedCategoryId) {
                    selectedCategoryId = current
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Карточка клуба ---
            item(key = "club_card") {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(club.name, style = MaterialTheme.typography.titleMedium)
                            Text(club.address, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { appVm.toggleFavoriteClub(club.id) }) {
                            Icon(
                                imageVector = if (appVm.isFavoriteClub(club.id))
                                    Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Избранное"
                            )
                        }
                    }
                }
            }

            // --- Поиск ---
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск по товарам и услугам") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // --- Чипы категорий ---
            item(key = "chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FakeData.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = cat.id
                                val idx = headerIndexByCatId[cat.id] ?: 0
                                scope.launch { listState.animateScrollToItem(idx) }
                            },
                            label = { Text(cat.title) }
                        )
                    }
                }
            }

            // --- Контент по всем категориям ---
            sectionsAll.forEach { (cat, list) ->
                item(key = "header_${cat.id}") {
                    Text(
                        text = cat.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                if (list.isEmpty()) {
                    item(key = "empty_${cat.id}") {
                        if (query.isNotBlank()) {
                            Text(
                                text = "Нет совпадений",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        } else {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                } else {
                    val rows = list.chunked(2)
                    rows.forEachIndexed { rowIndex, rowItems ->
                        item(key = "row_${cat.id}_$rowIndex") {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductTile(
                                    modifier = Modifier.weight(1f),
                                    product = rowItems[0],
                                    appVm = appVm,
                                    onOpenDetails = {
                                        if (!appVm.clubConfirmed) needConfirmClub = true
                                        sheetProduct = rowItems[0]
                                    },
                                    onPlusFirst = { p ->
                                        if (!appVm.clubConfirmed) needConfirmClub = true
                                        appVm.addProduct(p, null)
                                    },
                                    onMinus = { p -> appVm.changeQty(p.id, null, -1) },
                                    onPlus = { p -> appVm.changeQty(p.id, null, +1) }
                                )

                                if (rowItems.size > 1) {
                                    ProductTile(
                                        modifier = Modifier.weight(1f),
                                        product = rowItems[1],
                                        appVm = appVm,
                                        onOpenDetails = {
                                            if (!appVm.clubConfirmed) needConfirmClub = true
                                            sheetProduct = rowItems[1]
                                        },
                                        onPlusFirst = { p ->
                                            if (!appVm.clubConfirmed) needConfirmClub = true
                                            appVm.addProduct(p, null)
                                        },
                                        onMinus = { p -> appVm.changeQty(p.id, null, -1) },
                                        onPlus = { p -> appVm.changeQty(p.id, null, +1) }
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // чтобы последний ряд не прятался под кнопкой
            item(key = "bottom_spacer") { Spacer(Modifier.height(96.dp)) }
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

    // подтверждение клуба
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
            text = { Text("Ты сейчас заказываешь для клуба: ${club.name}") }
        )
    }

    // BottomSheet с подробным описанием
    sheetProduct?.let { p ->
        val sheetState = rememberModalBottomSheetState()

        val qtyInCart by remember(appVm.cartLines, p.id) {
            derivedStateOf {
                appVm.cartLines.firstOrNull { it.productId == p.id && it.variant == null }?.qty ?: 0
            }
        }

        ModalBottomSheet(
            onDismissRequest = { sheetProduct = null },
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
                    IconButton(onClick = { sheetProduct = null }) {
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
                            onMinus = { appVm.changeQty(p.id, null, -1) },
                            onPlus = { appVm.changeQty(p.id, null, +1) },
                            onAddFirst = { appVm.addProduct(p, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductTile(
    modifier: Modifier,
    product: Product,
    appVm: AppViewModel,
    onOpenDetails: () -> Unit,
    onPlusFirst: (Product) -> Unit,
    onMinus: (Product) -> Unit,
    onPlus: (Product) -> Unit
) {
    val qty by remember(appVm.cartLines, product.id) {
        derivedStateOf {
            appVm.cartLines.firstOrNull { it.productId == product.id && it.variant == null }?.qty ?: 0
        }
    }

    // Светлая тема (похоже на скрин)
    val cardBg = Color(0xFFFFFFFF)
    val imageBg = Color(0xFFF4F4F4)
    val pillBg = Color(0xFFEFEFEF)
    val textPrimary = Color(0xFF111111)
    val textSecondary = Color(0xFF7A7A7A)

    Card(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(24.dp)), // мягкая тень
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDetails() }
                .padding(14.dp)
                .heightIn(min = 320.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(imageBg),
                contentAlignment = Alignment.Center
            ) {
                // TODO: заменить на реальную картинку (Coil AsyncImage)
                Text("Фото", style = MaterialTheme.typography.labelLarge, color = textSecondary)
            }

            Text(
                text = "${product.price} ₽",
                color = textPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            Text(
                text = product.title,
                color = textPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = product.description,
                color = textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            ProductQtyBar(
                qty = qty,
                background = pillBg,
                textColor = textPrimary,
                onAddFirst = { onPlusFirst(product) },
                onMinus = { onMinus(product) },
                onPlus = { onPlus(product) }
            )
        }
    }
}

@Composable
private fun ProductQtyBar(
    qty: Int,
    background: Color,
    textColor: Color,
    onAddFirst: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        if (qty <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clickable { onAddFirst() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onMinus() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", style = MaterialTheme.typography.headlineSmall, color = textColor)
                }

                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onPlus() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall, color = textColor)
                }
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
