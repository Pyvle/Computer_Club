package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

@Composable
fun LoginScreen(
    appVm: AppViewModel,
    fromRoute: String,
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Вход", style = MaterialTheme.typography.headlineSmall)
        Text("После входа вернём тебя на: $fromRoute", style = MaterialTheme.typography.labelMedium)

        OutlinedTextField(username, { username = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val ok = appVm.login(username, password)
                if (ok) onSuccess() else error = "Проверь логин и пароль"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Войти") }

        TextButton(onClick = onGoRegister) { Text("Регистрация") }
    }
}
