package com.example.computerclub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.computerclub.app.AppNav
import com.example.computerclub.vm.AppViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.computerclub.ui.theme.ComputerClubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContent {
            val appVm: AppViewModel = viewModel()

            ComputerClubTheme(darkTheme = false, dynamicColor = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(appVm = appVm)
                }
            }
        }
    }
}
