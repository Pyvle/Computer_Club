package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.computerclub.R
import com.example.computerclub.data.network.dto.ClubProductResponseDto
import com.example.computerclub.data.network.dto.SeatPriceResponseDto
import com.example.computerclub.data.network.dto.SeatSpecResponseDto
import com.example.computerclub.data.network.dto.TimePackageResponseDto
import com.example.computerclub.ui.components.AppCard
import com.example.computerclub.ui.components.AppPrimaryButton
import com.example.computerclub.ui.components.AppSecondaryButton
import com.example.computerclub.ui.components.AppStatusChip
import com.example.computerclub.ui.components.ChipTone
import com.example.computerclub.ui.theme.*
import com.example.computerclub.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailsScreen(
    clubId: String,
    appVm: AppViewModel,
    onChosen: () -> Unit,
    onBack: () -> Unit,
    onReport: () -> Unit = {}
) {
    val club = appVm.clubs.firstOrNull { it.id == clubId }

    if (club == null) {
        Text("Клуб не найден")
        return
    }

    val clubIdLong = clubId.toLongOrNull() ?: return
    LaunchedEffect(clubIdLong) {
        appVm.loadClubDetailsExtras(clubIdLong)
    }

    val isFav = appVm.isFavoriteClub(club.id)
    val placeholder = painterResource(R.drawable.placeholder_club)

    val specs = appVm.clubDetailsSpecs
    val prices = appVm.clubDetailsPrices
    val packages = appVm.clubDetailsPackages
    val products = appVm.clubDetailsProducts

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = TextSecondary,
                    ),
                    title = {
                        Text(text = club.name, style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = TextSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onReport) {
                            Icon(
                                imageVector = Icons.Outlined.ReportProblem,
                                contentDescription = "Пожаловаться",
                                tint = TextSecondary
                            )
                        }
                        IconButton(onClick = { appVm.toggleFavoriteClub(club.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Избранное",
                                tint = if (isFav) BrandIndigo else TextSecondary
                            )
                        }
                    }
                )
                HorizontalDivider(color = AppBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Surface(color = AppSurface) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (club.isBlocked) {
                        club.blockReason?.let { reason ->
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    AppPrimaryButton(
                        text = if (club.isBlocked) "Заблокирован" else "Выбрать клуб",
                        onClick = {
                            appVm.chooseClub(club.id)
                            onChosen()
                        },
                        enabled = !club.isBlocked,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!club.isBlocked) {
                        AppSecondaryButton(
                            text = "Сообщить о проблеме",
                            onClick = onReport,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // hero-изображение
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
            )

            // белый контейнер с закруглёнными углами «наезжает» на изображение
            Column(
                modifier = Modifier
                    .offset(y = (-20).dp)
                    .clip(ShapeXL)
                    .fillMaxWidth()
                    .background(AppSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // название + статус
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = club.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (club.isBlocked) {
                        AppStatusChip(label = "Заблокирован", tone = ChipTone.ERROR)
                    }
                }

                // район
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = BrandIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = club.location,
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandIndigo
                    )
                }

                // адрес
                Text(
                    text = club.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                HorizontalDivider(color = AppBorder)

                // --- О клубе ---
                if (club.description.isNotBlank()) {
                    ClubSection(title = "О клубе") {
                        Text(
                            text = club.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // --- В клубе ---
                val amenities = buildAmenities(specs, prices, products)
                if (amenities.isNotEmpty()) {
                    ClubSection(title = "В клубе") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            amenities.forEach { (icon, label) ->
                                AmenityChip(icon = icon, label = label)
                            }
                        }
                    }
                }

                // --- Характеристики мест ---
                if (specs.isNotEmpty()) {
                    ClubSection(title = "Характеристики мест") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            specs.forEach { spec ->
                                SeatSpecCard(spec = spec)
                            }
                        }
                    }
                }

                // --- Стоимость ---
                if (prices.isNotEmpty()) {
                    ClubSection(title = "Стоимость") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            prices.filter { it.seatType == "REGULAR" }.firstOrNull()?.let { price ->
                                PriceCard(
                                    label = "Стандарт",
                                    pricePerHour = price.pricePerHourRub,
                                    isVip = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            prices.filter { it.seatType == "VIP" }.firstOrNull()?.let { price ->
                                PriceCard(
                                    label = "VIP",
                                    pricePerHour = price.pricePerHourRub,
                                    isVip = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // --- Пакеты времени ---
                if (packages.isNotEmpty()) {
                    ClubSection(title = "Пакеты времени") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            packages.forEach { pkg ->
                                TimePackageRow(pkg = pkg)
                            }
                        }
                    }
                }

                // --- Магазин (превью) ---
                if (products.isNotEmpty()) {
                    ClubSection(title = "Магазин") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            products.take(4).forEach { product ->
                                ProductPreviewRow(product = product)
                            }
                            if (products.size > 4) {
                                Text(
                                    text = "И ещё ${products.size - 4} товаров",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandIndigo,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // нижний отступ чтобы контент не перекрывался кнопками
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// --- Вспомогательные компоненты ---

@Composable
private fun ClubSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun AmenityChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(BrandIndigoSoft, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandIndigo,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BrandIndigo
        )
    }
}

@Composable
private fun SeatSpecCard(spec: SeatSpecResponseDto) {
    val isVip = spec.seatType == "VIP"
    val containerColor = if (isVip) Color(0xFFFFFBEB) else AppSurfaceAlt
    val borderColor = if (isVip) Color(0xFFFDE68A) else AppBorder
    val headerBg = if (isVip) Color(0xFFFEF3C7) else AppBackground
    val titleColor = if (isVip) StatusWarning else TextPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .background(containerColor)
    ) {
        // заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVip) Icons.Outlined.Star else Icons.Outlined.Computer,
                contentDescription = null,
                tint = if (isVip) StatusWarning else BrandIndigo,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = spec.title.ifBlank { if (isVip) "VIP места" else "Стандартные места" },
                style = MaterialTheme.typography.labelLarge,
                color = titleColor
            )
        }
        HorizontalDivider(color = borderColor, thickness = 1.dp)
        // строки характеристик
        spec.specs.forEachIndexed { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = line.value,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
            }
            if (index < spec.specs.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = borderColor,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun PriceCard(
    label: String,
    pricePerHour: Int,
    isVip: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isVip) Color(0xFFFFFBEB) else AppSurfaceAlt
    val border = if (isVip) Color(0xFFFDE68A) else AppBorder
    val textColor = if (isVip) StatusWarning else TextPrimary
    val labelColor = if (isVip) StatusWarning else TextSecondary

    Column(
        modifier = modifier
            .clip(ShapeMedium)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$pricePerHour",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Text(
                text = " ₽/ч",
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun TimePackageRow(pkg: TimePackageResponseDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeSmall)
            .background(AppSurfaceAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = pkg.name,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Text(
                text = "${pkg.hours} ч",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = "${pkg.totalPriceRub} ₽",
            style = MaterialTheme.typography.titleMedium,
            color = BrandIndigo
        )
    }
}

@Composable
private fun ProductPreviewRow(product: ClubProductResponseDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeSmall)
            .background(AppSurfaceAlt)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // иконка или изображение
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ShapeSmall)
                .background(BrandIndigoSoft),
            contentAlignment = Alignment.Center
        ) {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = BrandIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = product.categoryTitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Text(
            text = "${product.priceRub} ₽",
            style = MaterialTheme.typography.labelLarge,
            color = BrandIndigo
        )
    }
}

// --- Вспомогательная функция для генерации тегов "В клубе" ---

private fun buildAmenities(
    specs: List<SeatSpecResponseDto>,
    prices: List<SeatPriceResponseDto>,
    products: List<ClubProductResponseDto>
): List<Pair<ImageVector, String>> {
    val result = mutableListOf<Pair<ImageVector, String>>()
    result += Icons.Outlined.Computer to "Игровые ПК"
    result += Icons.Outlined.Wifi to "Wi-Fi"

    val hasRegular = specs.any { it.seatType == "REGULAR" } || prices.any { it.seatType == "REGULAR" }
    val hasVip = specs.any { it.seatType == "VIP" } || prices.any { it.seatType == "VIP" }
    if (hasRegular) result += Icons.Outlined.EventSeat to "Стандартные места"
    if (hasVip) result += Icons.Outlined.Star to "VIP-зона"
    if (products.isNotEmpty()) result += Icons.Outlined.ShoppingBag to "Магазин"

    // уникальные категории товаров (исключить уже добавленные как отдельные теги)
    products.map { it.categoryTitle }.distinct().forEach { cat ->
        if (result.none { (_, label) -> label == cat }) {
            result += Icons.Outlined.LocalCafe to cat
        }
    }
    return result
}
