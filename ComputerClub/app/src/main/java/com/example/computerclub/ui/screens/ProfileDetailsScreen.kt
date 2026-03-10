package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileDetailsScreen(appVm: AppViewModel, nav: NavHostController) {
    val u = appVm.user

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("← Назад") }
            Text("Профиль", style = MaterialTheme.typography.titleLarge)
        }

        if (u == null) {
            Text("Не авторизован.")
            return
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Телефон: ${u.phone}")
            }
        }

        Divider()
        Text("Смена пароля (заглушка)")
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Новый пароль") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { /* заглушка */ }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
    }
}
