package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Product
import com.example.computerclub.vm.AppViewModel

@Composable
fun ShopScreen(appVm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(FakeData.categories.first().id) }

    var productDialog: Product? by remember { mutableStateOf(null) }
    var needConfirmClub by remember { mutableStateOf(false) }

    val filtered = remember(query, selectedCategoryId) {
        FakeData.products
            .filter { it.categoryId == selectedCategoryId }
            .filter { it.title.contains(query, true) || it.description.contains(query, true) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // общий клуб (как в бронировании)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            // reuse internal function via BookingScreen isn't ideal; просто сделаем тут упрощённо:
            AssistChip(
                onClick = { /* клуб меняется здесь через диалог ниже */ },
                label = { Text("Клуб: " + FakeData.clubs.first { it.id == appVm.selectedClubId }.name) }
            )
            TextButton(onClick = { needConfirmClub = true }) { Text("Сменить/подтвердить") }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск по товарам и услугам") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // категории
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FakeData.categories) { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text(cat.title) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(filtered) { p ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(p.title, style = MaterialTheme.typography.titleMedium)
                            Text("${p.price} ₽", style = MaterialTheme.typography.labelLarge)
                            Text(p.description)
                        }
                        Button(onClick = {
                            if (!appVm.clubConfirmed) needConfirmClub = true
                            productDialog = p
                        }) { Text("+") }
                    }
                }
            }
        }

        Button(
            onClick = { /* переход в корзину через нижнюю панель */ },
            modifier = Modifier.fillMaxWidth()
        ) { Text("В корзину") }
    }

    // подтверждение клуба (чтобы не ошиблись)
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
            text = { Text("Ты сейчас заказываешь для клуба: ${FakeData.clubs.first { it.id == appVm.selectedClubId }.name}") }
        )
    }

    // окно товара
    productDialog?.let { p ->
        var variant by remember { mutableStateOf(p.variants.firstOrNull()) }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
