package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

@Composable
fun RegisterScreen(
    appVm: AppViewModel,
    fromRoute: String,
    onSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var smsSent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Регистрация", style = MaterialTheme.typography.headlineSmall)
        Text("Мок-код подтверждения: 1234", style = MaterialTheme.typography.labelMedium)

        OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(username, { username = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass1, { pass1 = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass2, { pass2 = it }, label = { Text("Повтори пароль") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { smsSent = true },
                enabled = phone.isNotBlank()
            ) { Text(if (smsSent) "Код отправлен" else "Отправить код") }

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter { ch -> ch.isDigit() } },
                label = { Text("Код") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val ok = appVm.register(phone, username, pass1, pass2, code)
                if (ok) onSuccess() else error = "Проверь поля и код"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Создать аккаунт") }

        TextButton(onClick = onGoLogin) { Text("Уже есть аккаунт? Войти") }
    }
}
