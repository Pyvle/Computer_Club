package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Product
import com.example.computerclub.vm.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(appVm: AppViewModel) {
    var query by remember { mutableStateOf("") }

    // выбранная категория (для подсветки чипов)
    var selectedCategoryId by remember { mutableStateOf(FakeData.categories.first().id) }

    var productDialog: Product? by remember { mutableStateOf(null) }
    var pendingProduct: Product? by remember { mutableStateOf(null) }
    var needConfirmClub by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val club = remember(appVm.selectedClubId) { FakeData.clubs.first { it.id == appVm.selectedClubId } }

    // Категории + товары по категориям, отфильтрованные по query
    val productsByCat = remember(query) {
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

    // Видимые секции: при поиске скрываем категории без совпадений
    val visibleSections = remember(productsByCat, query) {
        if (query.isBlank()) productsByCat
        else productsByCat.filter { (_, list) -> list.isNotEmpty() }
    }

    val visibleCatIds = remember(visibleSections) { visibleSections.map { it.first.id }.toSet() }

    // если выбранная категория пропала из-за поиска — переключаемся на первую доступную
    LaunchedEffect(visibleSections, query) {
        if (visibleSections.isEmpty()) return@LaunchedEffect
        if (selectedCategoryId !in visibleCatIds) {
            selectedCategoryId = visibleSections.first().first.id
            // чтобы пользователь сразу видел результаты
            listState.scrollToItem(0)
        }
    }

    // индекс заголовка каждой категории в LazyColumn (без stickyHeader)
    val headerIndexByCatId = remember(visibleSections) {
        val map = mutableMapOf<String, Int>()
        var idx = 0
        visibleSections.forEach { (cat, list) ->
            map[cat.id] = idx
            idx += 1 + list.size
        }
        map
    }

    // когда скроллим список — автоматически меняем подсветку категории (по текущему заголовку)
    LaunchedEffect(listState, visibleSections) {
        if (visibleSections.isEmpty()) return@LaunchedEffect

        val headers = visibleSections.mapNotNull { (cat, _) ->
            headerIndexByCatId[cat.id]?.let { cat.id to it }
        }.sortedBy { it.second }

        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIdx ->
                // найдём последний headerIndex <= firstIdx
                val current = headers.lastOrNull { it.second <= firstIdx }?.first
                if (current != null && current != selectedCategoryId) {
                    selectedCategoryId = current
                }
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // --- КЛУБ (как в бронировании) ---
        Card {
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

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск по товарам и услугам") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // --- Категории (при поиске показываем только подходящие) ---
        if (visibleSections.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleSections, key = { it.first.id }) { (cat, _) ->
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

        // --- Список товаров (одна лента) ---
        if (visibleSections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ничего не найдено")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                visibleSections.forEach { (cat, list) ->
                    // Заголовок категории
                    item(key = "header_${cat.id}") {
                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    // Товары категории
                    items(list, key = { it.id }) { p ->
                        Card {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${p.price} ₽", style = MaterialTheme.typography.labelLarge)
                                    Text(p.description)
                                }
                                Button(
                                    onClick = {
                                        if (!appVm.clubConfirmed) {
                                            pendingProduct = p
                                            needConfirmClub = true
                                        } else {
                                            productDialog = p
                                        }
                                    }
                                ) { Text("+") }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { /* переход в корзину через нижнюю панель */ },
            modifier = Modifier.fillMaxWidth()
        ) { Text("В корзину") }
    }

    // подтверждение клуба (мок)
    if (needConfirmClub) {
        AlertDialog(
            onDismissRequest = { needConfirmClub = false },
            confirmButton = {
                TextButton(onClick = {
                    appVm.confirmClub()
                    needConfirmClub = false
                    // после подтверждения — открываем товар, который хотели добавить
                    pendingProduct?.let { productDialog = it }
                    pendingProduct = null
                }) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    needConfirmClub = false
                    pendingProduct = null
                }) { Text("Отмена") }
            },
            title = { Text("Подтверди клуб") },
            text = { Text("Ты сейчас заказываешь для клуба: ${club.name}") }
        )
    }

    // окно товара
    productDialog?.let { p ->
        var variant by remember(p.id) { mutableStateOf(p.variants.firstOrNull()) }
        AlertDialog(
            onDismissRequest = { productDialog = null },
            confirmButton = {
                Button(onClick = {
                    appVm.addProduct(p, variant)
                    productDialog = null
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { productDialog = null }) { Text("Закрыть") } },
            title = { Text(p.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.description)
                    if (p.variants.isNotEmpty()) {
                        Text("Выбор:")
                        p.variants.forEach { v ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = variant == v, onClick = { variant = v })
                                Text(v)
                            }
                        }
                    }
                    Text("Цена: ${p.price} ₽")
                }
            }
        )
    }
}
