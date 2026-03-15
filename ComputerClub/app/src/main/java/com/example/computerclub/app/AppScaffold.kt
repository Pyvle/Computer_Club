package com.example.computerclub.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
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
    val showBottomBar = route.startsWith(Routes.Clubs) ||
            route.startsWith(Routes.Booking) ||
            route.startsWith(Routes.BookingSeats) ||
            route.startsWith(Routes.Shop) ||
            route.startsWith(Routes.Cart) ||
            route.startsWith(Routes.History) ||
            route.startsWith(Routes.Profile) ||
            route.startsWith(Routes.About)

    // Топбар скрываем на "Клубы" и "Корзина"
    val showTopBar = showBottomBar &&
            !route.startsWith(Routes.Clubs) &&
            !route.startsWith(Routes.Cart)

    // одинаковая top-level навигация для вкладок
    fun topLevelNavigate(dest: String) {
        nav.navigate(dest) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Routes.Clubs) { saveState = true }
        }
    }

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
                    route.startsWith(Routes.About) -> "О приложении"
                    route.startsWith(Routes.Profile) -> "Профиль"
                    else -> ""
                }

                ClubTopBar(
                    title = title,
                    isLoggedIn = appVm.isLoggedIn(),
                    onLoginClick = {
                        val safeFrom = when {
                            route.startsWith("club_details") -> Routes.Clubs
                            route.startsWith(Routes.BookingSeats) -> Routes.Booking
                            route.startsWith(Routes.ShopSearch) -> Routes.Shop
                            else -> route.substringBefore("?")
                        }
                        nav.navigate("login_phone?from=$safeFrom")
                    },
                    hideAuthAction = route.startsWith(Routes.Profile),
                    showBack = route.startsWith(Routes.BookingSeats) || route.startsWith(Routes.ShopSearch) || route.startsWith(Routes.History) || route.startsWith(Routes.About),
                    onBack = {
                        if (route.startsWith(Routes.ShopSearch) || route.startsWith(Routes.History) || route.startsWith(Routes.About)) {
                            nav.popBackStack()
                            return@ClubTopBar
                        }

                        val popped = nav.popBackStack(Routes.Booking, inclusive = false)
                        if (!popped) topLevelNavigate(Routes.Booking)
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                val current = when {
                    route.startsWith(Routes.BookingSeats) -> Routes.Booking
                    route.startsWith(Routes.ShopSearch) -> Routes.Shop
                    route.startsWith(Routes.History) -> Routes.Profile
                    route.startsWith(Routes.About) -> Routes.Profile
                    else -> route.substringBefore("?")
                }

                ClubBottomBar(
                    currentRoute = current,
                    onNavigate = { dest ->
                        // история открыта поверх профиля — кнопка Профиль просто возвращает назад
                        if (route.startsWith(Routes.History) && dest == Routes.Profile) {
                            nav.popBackStack()
                            return@ClubBottomBar
                        }
                        if (dest == current) return@ClubBottomBar

                        if (dest == Routes.Booking) {
                            val target = appVm.lastBookingRoute
                            nav.navigate(target) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.Clubs) { saveState = true }
                            }
                            return@ClubBottomBar
                        }

                        if (dest == Routes.Clubs) {
                            val popped = nav.popBackStack(Routes.Clubs, inclusive = false)
                            if (!popped) topLevelNavigate(Routes.Clubs)
                            return@ClubBottomBar
                        }

                        topLevelNavigate(dest)
                    }
                )
            }
        }
    ) { padding ->
        Surface(Modifier.padding(padding)) {
            content()
        }
    }
}
