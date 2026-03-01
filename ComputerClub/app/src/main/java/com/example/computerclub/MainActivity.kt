package com.example.computerclub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.computerclub.app.AppNav
import com.example.computerclub.ui.theme.ComputerClubTheme
import com.example.computerclub.vm.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContent {
            val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            val appVm: AppViewModel = viewModel(factory = factory)

            ComputerClubTheme(darkTheme = false, dynamicColor = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(appVm = appVm)
                }
            }
        }
    }
}