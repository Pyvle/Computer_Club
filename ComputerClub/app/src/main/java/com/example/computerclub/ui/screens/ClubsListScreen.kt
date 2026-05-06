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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.example.computerclub.R
import androidx.compose.ui.Modifier
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.FrameLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.computerclub.model.Club
import com.example.computerclub.vm.AppViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

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

    // один раз читаем favoriteClubIds как Set — O(1) lookup вместо O(n) contains per item
    val favoriteIds by remember { derivedStateOf { appVm.favoriteClubIds.toHashSet() } }

    val filteredAll by remember {
        derivedStateOf {
            if (query.isBlank()) all
            else all.filter { c ->
                c.name.contains(query, ignoreCase = true) ||
                        c.location.contains(query, ignoreCase = true) ||
                        c.address.contains(query, ignoreCase = true)
            }
        }
    }

    val favorites by remember { derivedStateOf { filteredAll.filter { it.id in favoriteIds } } }
    val currentList by remember { derivedStateOf { if (tab == ClubsTab.Favorites) favorites else filteredAll } }

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
            com.example.computerclub.ui.components.AppTextField(
                value = query,
                onValueChange = { query = it },
                label = "Поиск клуба / района / адреса",
                modifier = Modifier.weight(1f),
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
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = com.example.computerclub.ui.theme.BrandIndigo
            )
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Переключение вкладок (Избранные / Все)
        TabRow(
            selectedTabIndex = if (tab == ClubsTab.Favorites) 0 else 1,
            containerColor = com.example.computerclub.ui.theme.AppSurface,
            contentColor = com.example.computerclub.ui.theme.BrandIndigo,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (tab == ClubsTab.Favorites) 0 else 1]),
                    color = com.example.computerclub.ui.theme.BrandIndigo
                )
            }
        ) {
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
            com.example.computerclub.ui.components.AppEmptyState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "Нет избранных клубов",
                subtitle = "Добавляйте клубы в избранное, нажимая на закладку"
            )
            return
        }

        if (currentList.isEmpty()) {
            com.example.computerclub.ui.components.AppEmptyState(
                icon = Icons.Filled.ViewList,
                title = "Ничего не найдено",
                subtitle = "Попробуйте изменить запрос"
            )
            return
        }

        // ВЫБОР: список или карта
        if (mapMode) {
            ClubsMap(
                clubs = currentList,
                onClubClick = { onOpenClub(it.id) }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(currentList, key = { it.id }) { club ->
                    ClubCard(
                        club = club,
                        isFavorite = club.id in favoriteIds,
                        onToggleFavorite = { appVm.toggleFavoriteClub(club.id) },
                        onOpen = { if (!club.isBlocked) onOpenClub(club.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClubsMap(
    clubs: List<Club>,
    onClubClick: (Club) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onClubClickState = rememberUpdatedState(onClubClick)
    val clubsWithCoords = clubs.filter { it.latitude != null && it.longitude != null }

    if (clubsWithCoords.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(420.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Координаты клубов не указаны")
        }
        return
    }

    // сильные ссылки на слушатели — иначе GC соберёт JNI-объекты
    val tapListeners = remember { mutableListOf<MapObjectTapListener>() }
    val mapView = remember { MapView(context) }
    // якорь: нижний центр треугольника совпадает с координатой
    val iconStyle = remember { IconStyle().apply { anchor = PointF(0.5f, 1.0f) } }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                MapKitFactory.getInstance().onStart()
                mapView.onStart()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
                MapKitFactory.getInstance().onStop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply { addView(mapView) }
        },
        update = {
            val map = mapView.mapWindow.map
            map.mapObjects.clear()
            tapListeners.clear()

            val minLat = clubsWithCoords.minOf { it.latitude!! }
            val maxLat = clubsWithCoords.maxOf { it.latitude!! }
            val minLon = clubsWithCoords.minOf { it.longitude!! }
            val maxLon = clubsWithCoords.maxOf { it.longitude!! }
            val cameraPosition = if (clubsWithCoords.size == 1) {
                CameraPosition(Point(minLat, minLon), 14f, 0f, 0f)
            } else {
                // отступ, чтобы метки не упирались в края
                val pad = 0.05
                map.cameraPosition(
                    Geometry.fromBoundingBox(
                        BoundingBox(
                            Point(minLat - pad, minLon - pad),
                            Point(maxLat + pad, maxLon + pad)
                        )
                    )
                )
            }
            map.move(cameraPosition)

            clubsWithCoords.forEach { club ->
                val label = ImageProvider.fromBitmap(createClubLabelBitmap(context, club.name))
                val placemark = map.mapObjects.addPlacemark().apply {
                    geometry = Point(club.latitude!!, club.longitude!!)
                    setIcon(label, iconStyle)
                }
                val listener = MapObjectTapListener { _, _ ->
                    onClubClickState.value(club)
                    true
                }
                tapListeners.add(listener)
                placemark.addTapListener(listener)
            }
        },
        modifier = Modifier.fillMaxWidth().height(420.dp)
    )
}

@Composable
private fun ClubCard(
    club: Club,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    val placeholder = painterResource(R.drawable.placeholder_club)
    com.example.computerclub.ui.components.AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                // фиксированный aspect ratio — карточки не "прыгают" пока грузится фото
                AsyncImage(
                    model = club.imageUrl,
                    contentDescription = club.name,
                    placeholder = placeholder,
                    error = placeholder,
                    fallback = placeholder,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(com.example.computerclub.ui.theme.ShapeLarge)
                )

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = club.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (club.isBlocked) {
                            com.example.computerclub.ui.components.AppStatusChip(
                                label = "Заблокирован",
                                tone = com.example.computerclub.ui.components.ChipTone.ERROR
                            )
                        }
                    }
                    Text(
                        text = club.location,
                        style = MaterialTheme.typography.labelMedium,
                        color = com.example.computerclub.ui.theme.TextSecondary
                    )
                    Text(
                        text = club.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.example.computerclub.ui.theme.TextSecondary
                    )
                }
            }

            // кнопка избранного поверх изображения
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Избранное",
                    tint = if (isFavorite) com.example.computerclub.ui.theme.BrandIndigo
                    else com.example.computerclub.ui.theme.TextMuted
                )
            }
        }
    }
}

private fun createClubLabelBitmap(context: Context, text: String): Bitmap {
    val dp = context.resources.displayMetrics.density
    val padH = 14f * dp
    val padV = 8f * dp
    val tipH = 10f * dp   // высота треугольника-указателя
    val corner = 8f * dp

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * dp
        typeface = Typeface.DEFAULT_BOLD
    }

    val fm = textPaint.fontMetrics
    val textW = textPaint.measureText(text)
    val textH = fm.descent - fm.ascent

    val bmpW = (textW + padH * 2).toInt()
    val rectH = (textH + padV * 2).toInt()
    val bmpH = (rectH + tipH).toInt()

    val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }

    canvas.drawRoundRect(RectF(0f, 0f, bmpW.toFloat(), rectH.toFloat()), corner, corner, bgPaint)

    // треугольник по центру снизу
    canvas.drawPath(Path().apply {
        moveTo(bmpW / 2f - tipH, rectH.toFloat())
        lineTo(bmpW / 2f + tipH, rectH.toFloat())
        lineTo(bmpW / 2f, bmpH.toFloat())
        close()
    }, bgPaint)

    canvas.drawText(text, padH, padV - fm.ascent, textPaint)

    return bitmap
}
