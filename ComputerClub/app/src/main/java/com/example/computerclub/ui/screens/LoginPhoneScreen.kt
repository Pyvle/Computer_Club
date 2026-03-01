package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

@Composable
fun LoginPhoneScreen(
    appVm: AppViewModel,
    fromRoute: String,
    onGoCode: (challengeId: Long, phone: String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Вход по телефону", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Телефон") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val normalized = phone.trim()
                    .replace(" ", "")
                    .replace("-", "")
                    .replace("(", "")
                    .replace(")", "")

                appVm.authRequestOtp(
                    phone = normalized,
                    onSuccess = { challengeId ->
                        loading = false
                        onGoCode(challengeId, normalized)
                    },
                    onError = { }
                )
            },
            enabled = !loading && phone.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Отправляем..." else "Получить код")
        }

        Text(
            "Пароли не нужны. Мы отправим код по SMS.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}