package com.example.computerclub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.ui.theme.AppBorder
import com.example.computerclub.ui.theme.AppSurface
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.TextSecondary

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
    androidx.compose.foundation.layout.Column {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = TextSecondary,
                actionIconContentColor = BrandIndigo,
            ),
            navigationIcon = {
                if (showBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextSecondary
                        )
                    }
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (!hideAuthAction && !isLoggedIn) {
                        TextButton(
                            onClick = onLoginClick,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "Войти",
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandIndigo
                            )
                        }
                    }
                }
            }
        )
        HorizontalDivider(color = AppBorder, thickness = 1.dp)
    }
}
