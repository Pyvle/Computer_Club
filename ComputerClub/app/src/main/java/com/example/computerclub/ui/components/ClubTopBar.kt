package com.example.computerclub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubTopBar(
    title: String,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    hideAuthAction: Boolean = false,
    showBack: Boolean = false,
    onBack: () -> Unit = {}
) {
    TopAppBar(
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, modifier = Modifier.weight(1f))

                if (!hideAuthAction) {
                    if (!isLoggedIn) {
                        TextButton(onClick = onLoginClick) { Text("Войти") }
                    }
                    // когда залогинен — ничего не показываем (кошелька нет)
                }
            }
        }
    )
}
