package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.example.computerclub.ui.components.AppSecondaryButton
import com.example.computerclub.ui.components.AppTextField
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.TextSecondary
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

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = BrandIndigo,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "Введите код",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Код отправлен на номер\n$phone",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            AppTextField(
                value = code,
                onValueChange = {
                    code = it
                    error = null
                },
                label = "Код из SMS",
                isError = error != null,
                supportingText = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            AppPrimaryButton(
                text = if (loading) "Проверяем..." else "Войти",
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
                loading = loading,
                modifier = Modifier.fillMaxWidth()
            )

            AppSecondaryButton(
                text = "Назад",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
