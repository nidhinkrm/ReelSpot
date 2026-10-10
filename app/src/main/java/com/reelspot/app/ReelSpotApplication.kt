package com.reelspot.app

import android.app.Application
import com.reelspot.app.location.NotificationHelper

class ReelSpotApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
