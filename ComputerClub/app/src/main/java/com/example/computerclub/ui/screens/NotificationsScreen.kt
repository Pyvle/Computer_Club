package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.data.FakeData
import com.example.computerclub.vm.AppViewModel

@Composable
fun NotificationsScreen(appVm: AppViewModel, nav: NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("← Назад") }
            Text("Уведомления", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(FakeData.notifications) { n ->
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleMedium)
                        Text(n.text)
                    }
                }
            }
        }
    }
}
