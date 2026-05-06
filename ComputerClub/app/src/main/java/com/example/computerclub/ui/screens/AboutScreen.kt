package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.computerclub.ui.components.AppCard
import com.example.computerclub.ui.components.AppScreenContainer
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.TextSecondary

@Composable
fun AboutScreen() {
    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoRow("Название", "Приложение управления компьютерным клубом")
                    HorizontalDivider(color = AppBorder)
                    InfoRow("Версия", "1.0.0")
                    HorizontalDivider(color = AppBorder)
                    InfoRow("Платформа", "Android")
                }
            }

            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Описание", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Приложение для бронирования мест в компьютерных клубах. " +
                               "Позволяет выбрать клуб, забронировать место на удобное время, " +
                               "заказать товары и услуги, а также отслеживать историю своих заказов.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Возможности", style = MaterialTheme.typography.titleMedium)
                    listOf(
                        "Бронирование мест по схеме зала",
                        "Заказ еды и напитков",
                        "История заказов и бронирований",
"Избранные клубы",
                    ).forEach { feature ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("•", color = BrandIndigo, style = MaterialTheme.typography.bodyMedium)
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "© 2026 Приложение управления компьютерным клубом",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
