package com.example.computerclub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.computerclub.app.AppNav
import com.example.computerclub.vm.AppViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appVm: AppViewModel = viewModel()
            Surface(color = MaterialTheme.colorScheme.background) {
                AppNav(appVm = appVm)
            }
        }
    }
}
