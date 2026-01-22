package com.example.computerclub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Club
import com.example.computerclub.vm.AppViewModel
import kotlin.math.abs

private enum class ClubsTab { Favorites, All }

@Composable
fun ClubsListScreen(
    appVm: AppViewModel,
    onOpenClub: (clubId: String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(ClubsTab.All) } // можно Favorites, если хочешь
    var mapMode by rememberSaveable { mutableStateOf(false) }    // false = список, true = карта

    val all = FakeData.clubs

    val filteredAll = if (query.isBlank()) {
        all
    } else {
        all.filter { c ->
            c.name.contains(query, ignoreCase = true) ||
                    c.location.contains(query, ignoreCase = true) ||
                    c.address.contains(query, ignoreCase = true)
        }
    }

    val favorites = filteredAll.filter { appVm.isFavoriteClub(it.id) }
    val currentList = if (tab == ClubsTab.Favorites) favorites else filteredAll

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Поиск + кнопка Карта/Список справа
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск клуба / района / адреса") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            FilledTonalIconButton(
                onClick = { mapMode = !mapMode }
            ) {
                Icon(
                    imageVector = if (mapMode) Icons.Filled.ViewList else Icons.Filled.Map,
                    contentDescription = if (mapMode) "Список" else "Карта"
                )
            }
        }

        // Переключение вкладок (Избранные / Все)
        TabRow(selectedTabIndex = if (tab == ClubsTab.Favorites) 0 else 1) {
            Tab(
                selected = tab == ClubsTab.Favorites,
                onClick = { tab = ClubsTab.Favorites },
                text = { Text("Избранные") }
            )
            Tab(
                selected = tab == ClubsTab.All,
                onClick = { tab = ClubsTab.All },
                text = { Text("Все клубы") }
            )
        }

        if (tab == ClubsTab.Favorites && favorites.isEmpty()) {
            Text("Избранных клубов нет")
            return
        }

        if (currentList.isEmpty()) {
            Text("Ничего не найдено")
            return
        }

        // ВЫБОР: список или карта
        if (mapMode) {
            ClubsMapStub(
                clubs = currentList,
                onClubClick = { onOpenClub(it.id) }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(currentList, key = { it.id }) { club ->
                    val isFav = appVm.isFavoriteClub(club.id)
                    ClubCard(
                        club = club,
                        isFavorite = isFav,
                        onToggleFavorite = { appVm.toggleFavoriteClub(club.id) },
                        onOpen = { onOpenClub(club.id) }
                    )
                }
            }
        }
    }
}

/**
 * "Карта" заглушка: просто поле + "метки" клубов в виде chip'ов.
 * Позже легко заменить на Google Maps / Яндекс.
 */
@Composable
private fun ClubsMapStub(
    clubs: List<Club>,
    onClubClick: (Club) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) box@{
        Card(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Карта (заглушка)", style = MaterialTheme.typography.titleMedium)
                    Text("Тапни по метке клуба → откроются детали.", style = MaterialTheme.typography.bodyMedium)
                }

                clubs.forEach { club ->
                    val (xFrac, yFrac) = remember(club.id) { pseudoCoords(club.id) }

                    val x = this@box.maxWidth * xFrac
                    val y = this@box.maxHeight * yFrac

                    AssistChip(
                        onClick = { onClubClick(club) },
                        label = { Text(club.name) },
                        modifier = Modifier
                            .offset(x = x, y = y)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

private fun pseudoCoords(id: String): Pair<Float, Float> {
    // 0.12..0.82 чтобы метки не прилипали к краям
    val h = abs(id.hashCode())
    val x = 0.12f + ((h % 1000) / 1000f) * 0.70f
    val y = 0.18f + (((h / 1000) % 1000) / 1000f) * 0.65f
    return x to y
}

@Composable
private fun ClubCard(
    club: Club,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(club.name, style = MaterialTheme.typography.titleMedium)
                Text(club.location, style = MaterialTheme.typography.labelMedium)
                Text(club.address, style = MaterialTheme.typography.bodyMedium)
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Избранное"
                )
            }
        }
    }
}
