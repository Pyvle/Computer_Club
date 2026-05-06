package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.computerclub.ui.components.AppPrimaryButton
import com.example.computerclub.ui.components.AppScreenContainer
import com.example.computerclub.ui.components.AppTextField
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.TextSecondary
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

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // иконка
            Surface(
                shape = MaterialTheme.shapes.large,
                color = BrandIndigo,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "Вход по телефону",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Пароли не нужны. Мы отправим\nкод подтверждения по SMS.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            AppTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    error = null
                },
                label = "Номер телефона",
                placeholder = "+7 000 000-00-00",
                isError = error != null,
                supportingText = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            AppPrimaryButton(
                text = if (loading) "Отправляем..." else "Получить код",
                onClick = {
                    loading = true
                    error = null
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
                        onError = {
                            loading = false
                            error = it
                        }
                    )
                },
                enabled = !loading && phone.isNotBlank(),
                loading = loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
