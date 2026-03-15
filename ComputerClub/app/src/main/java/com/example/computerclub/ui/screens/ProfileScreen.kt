package com.example.computerclub.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.computerclub.app.Routes
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileScreen(appVm: AppViewModel, nav: NavHostController) {
    val loggedIn = appVm.isLoggedIn()
    val u = appVm.user

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

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) appVm.updateNotifications(true) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text("👤 ${u.phone}", style = MaterialTheme.typography.titleLarge)

        Card(onClick = { nav.navigate(Routes.History) }) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("История заказов", style = MaterialTheme.typography.titleMedium)
                    Text("Брони и покупки", style = MaterialTheme.typography.labelMedium)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Card {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Уведомления", style = MaterialTheme.typography.titleMedium)
                    Text("За 30 минут до брони", style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = appVm.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Switch
                            }
                        }
                        appVm.updateNotifications(enabled)
                    }
                )
            }
        }

        Card(onClick = { nav.navigate(Routes.About) }) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("О приложении", style = MaterialTheme.typography.titleMedium)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(onClick = { appVm.logout() }, modifier = Modifier.fillMaxWidth()) {
            Text("Выйти из аккаунта")
        }
    }
}
