package com.example.computerclub.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.computerclub.app.Routes

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
        BottomItem(Routes.History, Icons.Filled.History, "История"),
        BottomItem(Routes.Profile, Icons.Filled.Person, "Профиль"),
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
