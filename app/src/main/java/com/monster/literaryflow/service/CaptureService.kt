package com.monster.literaryflow.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID

class CaptureService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(1, NotificationCompat.Builder(this, SCREEN_CAPTURE_CHANNEL_ID).build())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
