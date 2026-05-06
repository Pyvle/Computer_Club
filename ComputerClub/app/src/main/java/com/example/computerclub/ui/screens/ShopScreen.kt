package com.example.computerclub.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.computerclub.model.Product
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShopScreen(
    appVm: AppViewModel,
    onOpenCart: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }

    var needConfirmClub by remember { mutableStateOf(false) }
    var sheetProduct by remember { mutableStateOf<Product?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val club = remember(appVm.selectedClubId, appVm.clubs) {
        appVm.clubs.firstOrNull { it.id == appVm.selectedClubId }
    }

    LaunchedEffect(appVm.selectedClubId, appVm.user, club?.isBlocked) {
        if (club?.isBlocked != true) {
            appVm.loadShopData(force = false)
        }
        // корзину синхронизируем только для авторизованных
        if (appVm.user != null && club?.isBlocked != true) {
            appVm.syncCartProducts(force = false)
        }
    }

    val categories = appVm.shopCategories
    val products = appVm.shopProducts

    if (club?.isBlocked == true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Вы заблокированы в этом клубе. Выберите другой клуб.")
        }
        return
    }

    if (appVm.shopLoading && categories.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    appVm.shopError?.let { err ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(err) }
            return
        }
    }

    // --- измеряем высоту липких чипов ---
    var chipsHeightPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val gapPx = remember(density) { with(density) { 8.dp.toPx().roundToInt() } }
    val fallbackChipsPx = remember(density) { with(density) { 56.dp.toPx().roundToInt() } }

    fun chipsTotalPx(): Int = (if (chipsHeightPx > 0) chipsHeightPx else fallbackChipsPx) + gapPx

    // scrollOffset в LazyList — сколько пикселей срезать сверху item
    // чтобы заголовок был ниже липких чипов, offset должен быть отрицательным
    fun scrollOffsetForHeader(): Int = -chipsTotalPx()

    // скрываем категории без товаров (нет товаров вообще или нет совпадений по поиску)
    val sectionsAll = remember(query, categories, products) {
        categories.map { cat ->
            val list = products
                .filter { it.categoryId == cat.id }
                .filter {
                    query.isBlank() ||
                            it.title.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            cat to list
        }.filter { (_, list) -> list.isNotEmpty() }
    }

    val visibleCategories = remember(sectionsAll) { sectionsAll.map { it.first } }

    LaunchedEffect(visibleCategories) {
        if (selectedCategoryId.isBlank() && visibleCategories.isNotEmpty()) {
            selectedCategoryId = visibleCategories.first().id
        }
    }

    // Элементы до категорий в LazyColumn: club_card, search, stickyHeader(chips)
    val headerOffset = 3

    // Индекс ЗАГОЛОВКА каждой категории в LazyColumn
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

    var isAutoScrolling by remember { mutableStateOf(false) }

    // плавно скроллим к заголовку категории так, чтобы он был виден под чипами
    suspend fun animateToCategory(catId: String) {
        val idx = headerIndexByCatId[catId] ?: return
        isAutoScrolling = true
        selectedCategoryId = catId

        listState.animateScrollToItem(
            index = idx,
            scrollOffset = scrollOffsetForHeader()
        )

        // микродоводка до идеала (1–2px, обычно незаметно)
        val desiredY = chipsTotalPx()
        repeat(2) {
            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == idx } ?: return@repeat
            val delta = (info.offset - desiredY).toFloat()
            if (abs(delta) <= 1f) return@repeat
            listState.scrollBy(delta)
        }

        isAutoScrolling = false
    }

    // подсветка категории при скролле по "линии под чипами"
    LaunchedEffect(listState, headerIndexByCatId, chipsHeightPx) {
        val headers = visibleCategories.mapNotNull { cat ->
            headerIndexByCatId[cat.id]?.let { cat.id to it }
        }.sortedBy { it.second }

        snapshotFlow {
            val anchorY = chipsTotalPx()
            val visible = listState.layoutInfo.visibleItemsInfo
                .filter { it.index >= headerOffset }

            visible.minByOrNull { abs(it.offset - anchorY) }?.index ?: headerOffset
        }
            .distinctUntilChanged()
            .collect { anchorIndex ->
                if (isAutoScrolling) return@collect
                val current = headers.lastOrNull { it.second <= anchorIndex }?.first
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // чтобы контент не прятался под нижней кнопкой
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // --- Карточка клуба ---
            item(key = "club_card") {
                com.example.computerclub.ui.components.AppCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(club?.name ?: "Клуб", style = MaterialTheme.typography.titleMedium)
                            Text(
                                club?.address ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = com.example.computerclub.ui.theme.TextSecondary
                            )
                        }
                        IconButton(onClick = { club?.let { appVm.toggleFavoriteClub(it.id) } }) {
                            val isFav = club != null && appVm.isFavoriteClub(club.id)
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Избранное",
                                tint = if (isFav) com.example.computerclub.ui.theme.BrandIndigo
                                       else com.example.computerclub.ui.theme.TextMuted
                            )
                        }
                    }
                }
            }

            // --- Поиск ---
            item(key = "search") {
                com.example.computerclub.ui.components.AppTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Поиск по товарам и услугам",
                )
            }

            // --- Липкие чипы ---
            stickyHeader(key = "chips") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .onSizeChanged { chipsHeightPx = it.height },
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleCategories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { scope.launch { animateToCategory(cat.id) } },
                                label = { Text(cat.title) }
                            )
                        }
                    }
                }
            }

            // --- Контент ---
            sectionsAll.forEach { (cat, list) ->
                item(key = "header_${cat.id}") {
                    Text(
                        text = cat.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                val rows = list.chunked(2)
                rows.forEachIndexed { rowIndex, rowItems ->
                    item(key = "row_${cat.id}_$rowIndex") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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

        // нижняя кнопка
        Surface(
            color = com.example.computerclub.ui.theme.AppSurfaceAlt,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                com.example.computerclub.ui.components.AppPrimaryButton(
                    text = "Перейти в корзину",
                    onClick = onOpenCart,
                    modifier = Modifier.fillMaxWidth()
                )
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
            text = { Text("Ты сейчас заказываешь для клуба: ${club?.name ?: "Клуб"}") }
        )
    }

    // BottomSheet
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
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
                        .clip(com.example.computerclub.ui.theme.ShapeXL)
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
                    shape = com.example.computerclub.ui.theme.ShapeLarge
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

    val imageBg = com.example.computerclub.ui.theme.AppSurfaceAlt
    val pillBg = com.example.computerclub.ui.theme.AppSurfaceAlt
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = com.example.computerclub.ui.theme.TextSecondary

    Card(
        modifier = modifier,
        shape = com.example.computerclub.ui.theme.ShapeLarge,
        colors = CardDefaults.cardColors(containerColor = com.example.computerclub.ui.theme.AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.computerclub.ui.theme.AppBorder)
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
                    .clip(com.example.computerclub.ui.theme.ShapeXL)
                    .background(imageBg),
                contentAlignment = Alignment.Center
            ) {
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
                Text("+", style = MaterialTheme.typography.headlineSmall, color = textColor)
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
                Text("+", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { onAddFirst() })
            } else {
                Text("−", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { onMinus() })
                Text(qty.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("+", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { onPlus() })
            }
        }
    }
}
