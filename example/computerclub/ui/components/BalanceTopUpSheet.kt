package com.example.computerclub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceTopUpSheet(
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onTopUp: (Int) -> Boolean
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Пополнение баланса", style = MaterialTheme.typography.titleLarge)

            if (!isLoggedIn) {
                Text("Сначала войдите в аккаунт, чтобы пополнять баланс.")
                Button(onClick = onDismiss) { Text("Ок") }
                Spacer(Modifier.height(12.dp))
                return@ModalBottomSheet
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { ch -> ch.isDigit() } },
                label = { Text("Сумма (₽)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    val amount = text.toIntOrNull() ?: 0
                    val ok = onTopUp(amount)
                    error = if (ok) null else "Введите корректную сумму"
                    if (ok) onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Пополнить") }

            Spacer(Modifier.height(12.dp))
        }
    }
}
