package com.monster.literaryflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.benjaminwan.ocrlibrary.OcrEngine
import com.monster.fastAccessibility.FastAccessibilityService

const val SCREEN_CAPTURE_CHANNEL_ID = "Screen Capture ID"
const val SCREEN_CAPTURE_CHANNEL_NAME = "Screen Capture"
class MyApp : Application() {
    companion object {
        lateinit var instance: Application
        lateinit var ocrEngine: OcrEngine
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        instance = this
        FastAccessibilityService.init(
            instance, MyAccessibilityService::class.java, arrayListOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_CLICKED,
            )
        )
        createScreenCaptureNotificationChannel()
        initOCREngine()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createScreenCaptureNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val screenCaptureChannel = NotificationChannel(SCREEN_CAPTURE_CHANNEL_ID, SCREEN_CAPTURE_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(screenCaptureChannel)
    }
    private fun initOCREngine() {
        ocrEngine = OcrEngine(this.applicationContext)
    }

}