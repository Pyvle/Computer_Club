package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.ui.components.AppPrimaryButton
import com.example.computerclub.ui.components.AppSecondaryButton
import com.example.computerclub.ui.components.AppTextField
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurface
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.StatusSuccess
import com.example.computerclub.ui.theme.TextSecondary
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
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = TextSecondary,
                    ),
                    title = {
                        Text("Сообщить о проблеме", style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = TextSecondary
                            )
                        }
                    }
                )
                HorizontalDivider(color = AppBorder, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            club?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandIndigo
                )
                Text(
                    text = it.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (sent) {
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Сообщение отправлено",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Спасибо! Мы рассмотрим вашу жалобу.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.weight(1f))
                AppSecondaryButton(
                    text = "Вернуться",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = "Описание проблемы",
                    placeholder = "Опишите проблему или замечание...",
                    isError = error != null,
                    supportingText = error,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

                Spacer(Modifier.weight(1f))

                AppPrimaryButton(
                    text = "Отправить",
                    onClick = {
                        if (text.isBlank()) {
                            error = "Введите текст сообщения"
                            return@AppPrimaryButton
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
                    loading = sending,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
