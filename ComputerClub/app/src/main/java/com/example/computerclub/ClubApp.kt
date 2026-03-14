package com.example.computerclub

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class ClubApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
    }
}
