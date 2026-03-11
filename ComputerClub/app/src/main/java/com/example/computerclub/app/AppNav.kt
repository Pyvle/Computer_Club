package com.example.computerclub.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.computerclub.ui.screens.*
import com.example.computerclub.vm.AppViewModel

@Composable
fun AppNav(appVm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.Splash

    // Чтобы вкладка "Бронь" в нижней панели возвращала на последний экран брони (время/места)
    LaunchedEffect(route) {
        appVm.onRouteChanged(route)
    }

    LaunchedEffect(Unit) {
        // сразу грузим клубы без авторизации — чтобы список был виден мгновенно
        appVm.loadClubs()
        // затем восстанавливаем сессию; если токен жив — перезагружаем клубы
        // с /clubs/available (даёт blocked-статус) и синхронизируем магазин/корзину
        appVm.loadMe(onSuccess = {
            appVm.loadClubs(force = true)
            appVm.loadShopData(force = true)
            appVm.syncCartProducts(force = true)
        })
    }

    // единый стиль навигации для верхнеуровневых экранов (как в нижней панели)
    fun navigateTopLevel(dest: String) {
        nav.navigate(dest) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Routes.Clubs) { saveState = true }
        }
    }

    AppScaffold(
        route = route,
        nav = nav,
        appVm = appVm
    ) {
        NavHost(navController = nav, startDestination = Routes.Splash) {

            composable(Routes.Splash) {
                SplashScreen(onDone = {
                    nav.navigate(Routes.Clubs) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                })
            }

            composable(Routes.Clubs) {
                ClubsListScreen(
                    appVm = appVm,
                    onOpenClub = { id -> nav.navigate("club_details/$id") }
                )
            }

            composable(
                route = Routes.ClubDetails,
                arguments = listOf(navArgument("clubId") { type = NavType.StringType })
            ) { entry ->
                val clubId = entry.arguments?.getString("clubId") ?: ""
                ClubDetailsScreen(
                    clubId = clubId,
                    appVm = appVm,
                    onBack = { nav.popBackStack() },
                    onChosen = {
                        nav.navigate(Routes.Booking) { launchSingleTop = true }
                    }
                )
            }

            composable(Routes.Booking) {
                BookingSetupScreen(
                    appVm = appVm,
                    onNext = { nav.navigate(Routes.BookingSeats) },
                    // в корзину — строго top-level
                    onQuickBookToCart = { navigateTopLevel(Routes.Cart) }
                )
            }

            composable(Routes.BookingSeats) {
                BookingSeatsScreen(
                    appVm = appVm,
                    // в корзину — строго top-level
                    onGoToCart = { navigateTopLevel(Routes.Cart) }
                )
            }

            composable(Routes.Shop) {
                ShopScreen(
                    appVm = appVm,
                    // в корзину — строго top-level, иначе нижняя панель “залипает”
                    onOpenCart = { navigateTopLevel(Routes.Cart) }
                )
            }

            composable(Routes.ShopSearch) {
                ShopSearchScreen(appVm = appVm)
            }

            composable(Routes.Cart) {
                CartScreen(
                    appVm = appVm,
                    onEditBooking = { lineId ->
                        if (appVm.beginEditBooking(lineId)) {
                            navigateTopLevel(Routes.Booking)
                        }
                    },
                    onPaid = { navigateTopLevel(Routes.History) },
                    onLoginRequired = { nav.navigate("login_phone?from=${Routes.Cart}") }
                )
            }

            composable(Routes.History) { HistoryScreen(appVm = appVm) }
            composable(Routes.Profile) { ProfileScreen(appVm = appVm, nav = nav) }

            composable(
                route = Routes.LoginPhone,
                arguments = listOf(navArgument("from") { type = NavType.StringType; defaultValue = Routes.Clubs })
            ) { entry ->
                val from = entry.arguments?.getString("from") ?: Routes.Clubs
                LoginPhoneScreen(
                    appVm = appVm,
                    fromRoute = from,
                    onGoCode = { challengeId, phone ->
                        nav.navigate("login_code?from=$from&phone=$phone&challengeId=$challengeId")
                    }
                )
            }

            composable(
                route = Routes.LoginCode,
                arguments = listOf(
                    navArgument("from") { type = NavType.StringType; defaultValue = Routes.Clubs },
                    navArgument("phone") { type = NavType.StringType; defaultValue = "" },
                    navArgument("challengeId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                val from = entry.arguments?.getString("from") ?: Routes.Clubs
                val phone = entry.arguments?.getString("phone") ?: ""
                val challengeId = entry.arguments?.getLong("challengeId") ?: 0L

                LoginCodeScreen(
                    appVm = appVm,
                    fromRoute = from,
                    phone = phone,
                    challengeId = challengeId,
                    onSuccess = {
                        nav.navigate(from) {
                            popUpTo(Routes.LoginPhone) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.Notifications) { NotificationsScreen(appVm = appVm, nav = nav) }
            composable(Routes.ProfileDetails) { ProfileDetailsScreen(appVm = appVm, nav = nav) }
        }
    }
}
