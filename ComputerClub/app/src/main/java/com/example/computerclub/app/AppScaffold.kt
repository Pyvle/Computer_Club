package com.example.computerclub.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.computerclub.ui.components.BalanceTopUpSheet
import com.example.computerclub.ui.components.ClubBottomBar
import com.example.computerclub.ui.components.ClubTopBar
import com.example.computerclub.vm.AppViewModel

@Composable
fun AppScaffold(
    route: String,
    nav: NavHostController,
    appVm: AppViewModel,
    content: @Composable () -> Unit
) {
    // Нижняя панель видна и на booking_seats тоже
    val showBottomBar = route.startsWith(Routes.Clubs) ||
            route.startsWith(Routes.Booking) ||
            route.startsWith(Routes.BookingSeats) ||
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
                    route.startsWith(Routes.BookingSeats) -> "Бронирование"
                    route.startsWith(Routes.ShopSearch) -> "Поиск"
                    route.startsWith(Routes.Shop) -> "Товары и услуги"
                    route.startsWith(Routes.Cart) -> "Корзина"
                    route.startsWith(Routes.History) -> "История"
                    route.startsWith(Routes.Profile) -> "Профиль"
                    else -> ""
                }

                ClubTopBar(
                    title = title,
                    isLoggedIn = appVm.isLoggedIn(),
                    balance = appVm.balance,
                    onBalanceClick = { topUpOpen = true },
                    onLoginClick = {
                        nav.navigate("login?from=${route.substringBefore("?")}")
                    },
                    hideAuthAction = route.startsWith(Routes.Profile),
                    // стрелка назад на booking_seats и на экране поиска
                    showBack = route.startsWith(Routes.BookingSeats) || route.startsWith(Routes.ShopSearch),
                    onBack = {
                        if (route.startsWith(Routes.ShopSearch)) {
                            nav.popBackStack()
                            return@ClubTopBar
                        }

                        val popped = nav.popBackStack(Routes.Booking, inclusive = false)
                        if (!popped) {
                            nav.navigate(Routes.Booking) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(nav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                // Подсветка вкладки "Бронь" и на booking_seats тоже
                val current = when {
                    route.startsWith(Routes.BookingSeats) -> Routes.Booking
                    route.startsWith(Routes.ShopSearch) -> Routes.Shop
                    else -> route.substringBefore("?")
                }

                ClubBottomBar(
                    currentRoute = current,
                    onNavigate = { dest ->
                        // Вкладка "Бронь" должна возвращать на последний экран брони (время/места),
                        // чтобы экран выбора мест не "сбрасывался" при переходах по нижней панели.
                        if (dest == Routes.Booking) {
                            val target = appVm.lastBookingRoute
                            nav.navigate(target) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(nav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                            return@ClubBottomBar
                        }

                        // ✅ ВАЖНО: "Клубы" должны работать из брони всегда.
                        if (dest == Routes.Clubs) {
                            // 1) Если "clubs" есть в back stack — просто возвращаемся к нему.
                            val popped = nav.popBackStack(Routes.Clubs, inclusive = false)
                            if (!popped) {
                                // 2) Иначе — обычная навигация.
                                nav.navigate(Routes.Clubs) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            }
                            return@ClubBottomBar
                        }

                        // Для остальных вкладок — обычная схема (как у тебя было)
                        val clubsNodeId = nav.graph.findNode(Routes.Clubs)?.id
                        nav.navigate(dest) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(clubsNodeId ?: nav.graph.findStartDestination().id) {
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
