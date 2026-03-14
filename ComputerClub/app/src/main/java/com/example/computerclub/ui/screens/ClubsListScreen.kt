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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.computerclub.model.Club
import com.example.computerclub.vm.AppViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView

private enum class ClubsTab { Favorites, All }

@Composable
fun ClubsListScreen(
    appVm: AppViewModel,
    onOpenClub: (clubId: String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(ClubsTab.All) } // можно Favorites, если хочешь
    var mapMode by rememberSaveable { mutableStateOf(false) }    // false = список, true = карта

    val all = appVm.clubs
    val loading = appVm.clubsLoading
    val error = appVm.clubsError

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

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
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
            DGisClubsMap(
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
                        onOpen = { if (!club.isBlocked) onOpenClub(club.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DGisClubsMap(
    clubs: List<Club>,
    onClubClick: (Club) -> Unit
) {
    val geoClubs = remember(clubs) {
        clubs.filter { it.latitude != null && it.longitude != null }
    }
    val currentOnClubClick = rememberUpdatedState(onClubClick)
    val mapRef = remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).also { mv -> mapRef.value = mv }
        },
        update = { mv ->
            val map = mv.mapWindow.map
            // сбрасываем старые маркеры и рисуем по актуальному фильтру
            map.mapObjects.clear()

            if (geoClubs.isNotEmpty()) {
                val avgLat = geoClubs.sumOf { it.latitude!! } / geoClubs.size
                val avgLng = geoClubs.sumOf { it.longitude!! } / geoClubs.size
                map.move(CameraPosition(Point(avgLat, avgLng), 10f, 0f, 0f))
            }

            geoClubs.forEach { club ->
                val placemark = map.mapObjects.addPlacemark().apply {
                    geometry = Point(club.latitude!!, club.longitude!!)
                    userData = club
                }
                placemark.addTapListener { _, _ ->
                    currentOnClubClick.value(club)
                    true
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    MapKitFactory.getInstance().onStart()
                    mapRef.value?.onStart()
                }
                Lifecycle.Event.ON_STOP -> {
                    MapKitFactory.getInstance().onStop()
                    mapRef.value?.onStop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        if (club.imageUrl != null) {
            AsyncImage(
                model = club.imageUrl,
                contentDescription = club.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (club.isBlocked) {
                    Text(
                        text = "Заблокирован${club.blockReason?.let { ": $it" } ?: ""}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
