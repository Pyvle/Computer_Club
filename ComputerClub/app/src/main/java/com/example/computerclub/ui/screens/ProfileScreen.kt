package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.app.Routes
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileScreen(appVm: AppViewModel, nav: NavHostController) {
    val loggedIn = appVm.isLoggedIn()
    val u = appVm.user  // НЕ !!, может быть null

    // если не вошёл или user ещё не загружен — показываем кнопку входа и выходим
    if (!loggedIn || u == null) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Войди, чтобы синхронизировать профиль и историю заказов.")

            Button(
                onClick = { nav.navigate("login_phone?from=${Routes.Profile}") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти по телефону")
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Card(onClick = { nav.navigate(Routes.ProfileDetails) }) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👤 ${u.username}", style = MaterialTheme.typography.titleLarge)
                Text("Персональные данные", style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Уведомления", style = MaterialTheme.typography.titleMedium)
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