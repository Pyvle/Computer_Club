package com.example.computerclub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.model.Club
import com.example.computerclub.vm.AppViewModel

@Composable
fun ClubsListScreen(
    appVm: AppViewModel,
    onOpenClub: (clubId: String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val all = FakeData.clubs

    // фильтрация
    val filtered = remember(all, query) {
        if (query.isBlank()) all
        else all.filter { c ->
            c.name.contains(query, ignoreCase = true) ||
                    c.location.contains(query, ignoreCase = true) ||
                    c.address.contains(query, ignoreCase = true)
        }
    }

    val favorites = filtered.filter { appVm.isFavoriteClub(it.id) }
    val regular = filtered.filterNot { appVm.isFavoriteClub(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск клуба / района / адреса") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            if (favorites.isNotEmpty()) {
                item {
                    Text("Избранные", style = MaterialTheme.typography.titleMedium)
                }
                items(favorites, key = { it.id }) { club ->
                    ClubCard(
                        club = club,
                        isFavorite = true,
                        onToggleFavorite = { appVm.toggleFavoriteClub(club.id) },
                        onOpen = { onOpenClub(club.id) }
                    )
                }
                item { Spacer(Modifier.height(6.dp)) }
            }

            item {
                Text("Все клубы", style = MaterialTheme.typography.titleMedium)
            }
            items(regular, key = { it.id }) { club ->
                ClubCard(
                    club = club,
                    isFavorite = false,
                    onToggleFavorite = { appVm.toggleFavoriteClub(club.id) },
                    onOpen = { onOpenClub(club.id) }
                )
            }

            if (favorites.isEmpty() && regular.isEmpty()) {
                item {
                    Text("Ничего не найдено")
                }
            }
        }
    }
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
