package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("О приложении", style = MaterialTheme.typography.headlineSmall)

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Название", "Computer Club")
                InfoRow("Версия", "1.0.0")
                InfoRow("Платформа", "Android")
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Описание", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Приложение для бронирования мест в компьютерных клубах. " +
                    "Позволяет выбрать клуб, забронировать место на удобное время, " +
                    "заказать товары и услуги, а также отслеживать историю своих заказов.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Возможности", style = MaterialTheme.typography.titleMedium)
                FeatureItem("Бронирование мест по схеме зала")
                FeatureItem("Заказ еды и напитков")
                FeatureItem("История заказов и бронирований")
                FeatureItem("Уведомления перед началом брони")
                FeatureItem("Избранные клубы")
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "© 2026 Computer Club",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FeatureItem(text: String) {
    Text("• $text", style = MaterialTheme.typography.bodyMedium)
}
