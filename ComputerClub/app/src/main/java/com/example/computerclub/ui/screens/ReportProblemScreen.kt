package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(
    clubId: String,
    appVm: AppViewModel,
    onBack: () -> Unit
) {
    val club = appVm.clubs.firstOrNull { it.id == clubId }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщить о проблеме") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (club != null) {
                Text(
                    text = "Клуб: ${club.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (sent) {
                Text(
                    text = "Сообщение отправлено. Спасибо!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Вернуться")
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = { Text("Опишите проблему или замечание...") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )

                Button(
                    onClick = {
                        if (text.isBlank()) {
                            error = "Введите текст сообщения"
                            return@Button
                        }
                        sending = true
                        appVm.submitReport(
                            clubId = clubId,
                            message = text.trim(),
                            onSuccess = { sending = false; sent = true },
                            onError = { msg -> sending = false; error = msg }
                        )
                    },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Отправить")
                    }
                }
            }
        }
    }
}
