package com.example.computerclub.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.computerclub.ui.components.BalanceTopUpSheet
import com.example.computerclub.ui.components.ClubBottomBar
import com.example.computerclub.ui.components.ClubTopBar
import com.example.computerclub.vm.AppViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun AppScaffold(
    route: String,
    nav: NavHostController,
    appVm: AppViewModel,
    content: @Composable () -> Unit
) {
    val showBottomBar = route.startsWith(Routes.Clubs) ||
            route.startsWith(Routes.Booking) ||
            route.startsWith(Routes.Shop) ||
            route.startsWith(Routes.Cart) ||
            route.startsWith(Routes.History) ||
            route.startsWith(Routes.Profile)

    val showTopBar = showBottomBar && !route.startsWith(Routes.Clubs)

    var topUpOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                val title = when {
                    route.startsWith(Routes.Clubs) -> "Клубы"
                    route.startsWith("club_details") -> "Клуб"
                    route.startsWith(Routes.Booking) -> "Бронирование"
                    route.startsWith(Routes.Shop) -> "Товары и услуги"
                    route.startsWith(Routes.Cart) -> "Корзина"
                    route.startsWith(Routes.History) -> "История"
                    route.startsWith(Routes.Profile) -> "Профиль"
                    else -> ""
                }

                val searchEnabled = !route.startsWith(Routes.Booking) && !route.startsWith(Routes.Cart) && !route.startsWith(Routes.History) && !route.startsWith(Routes.Profile)
                // (можно включить поиск и там — но ты писал, что в бронировании его нет)

                ClubTopBar(
                    title = title,
                    isLoggedIn = appVm.isLoggedIn(),
                    balance = appVm.balance,
                    onBalanceClick = { topUpOpen = true },
                    onLoginClick = {
                        // логин “от текущего” (тут достаточно route без аргументов)
                        nav.navigate("login?from=${route.substringBefore("?")}")
                    },
                    hideAuthAction = route.startsWith(Routes.Profile)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                ClubBottomBar(
                    currentRoute = route.substringBefore("?"),
                    onNavigate = { dest ->
                        nav.navigate(dest) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Surface(Modifier.padding(padding)) {
            content()
        }
    }

    if (topUpOpen) {
        BalanceTopUpSheet(
            isLoggedIn = appVm.isLoggedIn(),
            onDismiss = { topUpOpen = false },
            onTopUp = { amount -> appVm.topUp(amount) }
        )
    }
}
