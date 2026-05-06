package com.example.computerclub.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.computerclub.app.Routes
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurface
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.BrandIndigoSoft
import com.example.computerclub.ui.theme.TextSecondary

private data class BottomItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun ClubBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomItem(Routes.Clubs, Icons.Filled.Home, "Клубы"),
        BottomItem(Routes.Booking, Icons.Filled.DateRange, "Бронь"),
        BottomItem(Routes.Shop, Icons.Filled.List, "Товары"),
        BottomItem(Routes.Cart, Icons.Filled.ShoppingCart, "Корзина"),
        BottomItem(Routes.Profile, Icons.Filled.Person, "Профиль"),
    )

    Column {
        HorizontalDivider(color = AppBorder, thickness = 1.dp)
        NavigationBar(containerColor = AppSurface) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandIndigo,
                        selectedTextColor = BrandIndigo,
                        indicatorColor = BrandIndigoSoft,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                    )
                )
            }
        }
    }
}
