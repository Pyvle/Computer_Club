package com.example.computerclub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.computerclub.app.Routes
import com.example.computerclub.ui.components.AppPrimaryButton
import com.example.computerclub.ui.components.AppScreenContainer
import com.example.computerclub.ui.theme.BrandIndigo
import com.example.computerclub.ui.theme.TextSecondary
import com.example.computerclub.vm.AppViewModel

@Composable
fun ProfileScreen(appVm: AppViewModel, nav: NavHostController) {
    val user = appVm.user

    if (user != null) {
        ProfileDetailsScreen(appVm = appVm, nav = nav)
        return
    }

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.height(72.dp),
                tint = BrandIndigo
            )
            Text(
                text = "Войдите в аккаунт",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Синхронизируйте профиль, избранные клубы и историю заказов.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            AppPrimaryButton(
                text = "Войти по телефону",
                onClick = { nav.navigate("login_phone?from=${Routes.Profile}") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
