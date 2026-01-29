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
                        // после выбора — логично отправить в бронирование
                        nav.navigate(Routes.Booking) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.Booking) {
                BookingSetupScreen(
                    appVm = appVm,
                    onNext = { nav.navigate(Routes.BookingSeats) },
                    onQuickBookToCart = { nav.navigate(Routes.Cart) }
                )
            }

            composable(Routes.BookingSeats) {
                BookingSeatsScreen(
                    appVm = appVm,
                    onGoToCart = {
                        nav.navigate(Routes.Cart)
                    }
                )
            }

            composable(Routes.Shop) {
                ShopScreen(
                    appVm = appVm,
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
                            nav.navigate(Routes.Booking) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
            composable(Routes.History) { HistoryScreen(appVm = appVm) }
            composable(Routes.Profile) { ProfileScreen(appVm = appVm, nav = nav) }

            composable(
                route = Routes.Login,
                arguments = listOf(navArgument("from") { type = NavType.StringType; defaultValue = Routes.Clubs })
            ) { entry ->
                val from = entry.arguments?.getString("from") ?: Routes.Clubs
                LoginScreen(
                    appVm = appVm,
                    fromRoute = from,
                    onSuccess = {
                        nav.navigate(from) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoRegister = { nav.navigate("register?from=$from") }
                )
            }

            composable(
                route = Routes.Register,
                arguments = listOf(navArgument("from") { type = NavType.StringType; defaultValue = Routes.Clubs })
            ) { entry ->
                val from = entry.arguments?.getString("from") ?: Routes.Clubs
                RegisterScreen(
                    appVm = appVm,
                    fromRoute = from,
                    onSuccess = {
                        nav.navigate(from) {
                            popUpTo(Routes.Register) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoLogin = { nav.navigate("login?from=$from") }
                )
            }

            composable(Routes.Notifications) { NotificationsScreen(appVm = appVm, nav = nav) }
            composable(Routes.ProfileDetails) { ProfileDetailsScreen(appVm = appVm, nav = nav) }
        }
    }
}
