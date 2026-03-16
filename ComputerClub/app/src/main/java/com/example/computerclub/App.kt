package com.example.computerclub

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // setApiKey обязательно вызывается до initialize
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
    }
}
