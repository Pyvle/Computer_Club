package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.ui.components.AppCard
import com.example.computerclub.ui.components.AppScreenContainer
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.BrandIndigoSoft
import com.example.computerclub.ui.theme.TextSecondary
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileDetailsScreen(appVm: AppViewModel, nav: NavHostController) {
    val u = appVm.user

    AppScreenContainer {
        if (u == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Не авторизован.", color = TextSecondary)
            }
            return@AppScreenContainer
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // акцентный блок с номером телефона
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = BrandIndigoSoft, shape = MaterialTheme.shapes.large)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = BrandIndigo, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = u.phone,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Пользователь",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Данные аккаунта", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(color = AppBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Телефон", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(u.phone, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
