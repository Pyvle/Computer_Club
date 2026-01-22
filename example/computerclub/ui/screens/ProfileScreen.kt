package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.app.Routes
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileScreen(appVm: AppViewModel, nav: NavHostController) {
    val loggedIn = appVm.isLoggedIn()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!loggedIn) {
            Text("Войди или зарегистрируйся, чтобы видеть баланс и историю.")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { nav.navigate("login?from=${Routes.Profile}") }, modifier = Modifier.weight(1f)) { Text("Войти") }
                OutlinedButton(onClick = { nav.navigate("register?from=${Routes.Profile}") }, modifier = Modifier.weight(1f)) { Text("Регистрация") }
            }
            Divider()
            Text("Разделы (пока заглушка): акции / поддержка / клубы")
            return
        }

        // Авторизован
        val u = appVm.user!!
        Card(onClick = { nav.navigate(Routes.ProfileDetails) }) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👤 ${u.username}", style = MaterialTheme.typography.titleLarge)
                Text("Персональные данные", style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Баланс: ${appVm.balance} ₽", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { nav.navigate(Routes.Notifications) }) {
                Text("🔔")
            }
        }

        Divider()

        Text("Разделы:", style = MaterialTheme.typography.titleMedium)
        ListItem(headlineContent = { Text("Акции") }, supportingContent = { Text("Заглушка") })
        ListItem(headlineContent = { Text("Поддержка") }, supportingContent = { Text("Заглушка (чат позже)") })
        ListItem(headlineContent = { Text("Клубы") }, supportingContent = { Text("Заглушка") })

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { appVm.logout() }, modifier = Modifier.fillMaxWidth()) {
            Text("Выйти из аккаунта")
        }
    }
}
