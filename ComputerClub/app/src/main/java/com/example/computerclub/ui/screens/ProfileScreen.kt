package com.example.computerclub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.app.Routes
import com.example.computerclub.ui.components.AppCard
import com.example.computerclub.ui.components.AppPrimaryButton
import com.example.computerclub.ui.components.AppSecondaryButton
import com.example.computerclub.ui.components.AppScreenContainer
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.BrandIndigoSoft
import com.example.computerclub.ui.theme.TextSecondary
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileScreen(appVm: AppViewModel, nav: NavHostController) {
    val loggedIn = appVm.isLoggedIn()
    val u = appVm.user

    AppScreenContainer {
        if (!loggedIn || u == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = BrandIndigo
                )
                Text(
                    text = "Войдите в аккаунт",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Синхронизируйте профиль и историю заказов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                AppPrimaryButton(
                    text = "Войти по телефону",
                    onClick = { nav.navigate("login_phone?from=${Routes.Profile}") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                Text(
                    text = u.phone,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            AppCard(onClick = { nav.navigate(Routes.History) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("История заказов", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Брони и покупки",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            AppCard(onClick = { nav.navigate(Routes.About) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("О приложении", style = MaterialTheme.typography.titleMedium)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            AppSecondaryButton(
                text = "Выйти из аккаунта",
                onClick = { appVm.logout() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
