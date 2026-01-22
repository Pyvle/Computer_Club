package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

@Composable
fun HistoryScreen(appVm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Текущая сессия") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("История") })
        }

        if (tab == 0) {
            val s = appVm.currentSession
            if (s == null) {
                Text("Сейчас нет активной сессии (мок).")
            } else {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Клуб: ${s.clubName}", style = MaterialTheme.typography.titleMedium)
                        Text("Места: ${s.seatLabels.joinToString()}")
                        Text("Начало: ${s.startLabel}")
                        Text("Конец: ${s.endLabel}")
                        Text("Осталось: ${s.remainingLabel}")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Заказы (мок): нет / в работе", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text("История бронирований/заказов (заглушка)")
            Spacer(Modifier.height(8.dp))
            Card { Column(Modifier.padding(12.dp)) { Text("Сессия 1 — вчера (мок)") } }
            Card { Column(Modifier.padding(12.dp)) { Text("Заказ 1 — выполнен (мок)") } }
        }
    }
}
