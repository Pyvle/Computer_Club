package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

@Composable
fun LoginCodeScreen(
    appVm: AppViewModel,
    fromRoute: String,
    phone: String,
    challengeId: Long,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Подтверждение", style = MaterialTheme.typography.titleLarge)
        Text("Код отправлен на: $phone", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Код из SMS") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                loading = true
                error = null
                appVm.authVerifyOtp(
                    challengeId = challengeId,
                    code = code,
                    onSuccess = {
                        loading = false
                        onSuccess()
                    },
                    onError = {
                        loading = false
                        error = it
                    }
                )
            },
            enabled = !loading && code.length >= 3,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Проверяем..." else "Войти")
        }

        TextButton(onClick = onBack) { Text("Назад") }
    }
}