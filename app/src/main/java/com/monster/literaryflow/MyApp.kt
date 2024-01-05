package com.monster.literaryflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.benjaminwan.ocrlibrary.OcrEngine
import com.monster.fastAccessibility.FastAccessibilityService


const val SCREEN_CAPTURE_CHANNEL_ID = "Screen Capture ID"
const val SCREEN_CAPTURE_CHANNEL_NAME = "Screen Capture"
const val SCREEN_CAPTURE_CHANNEL_ID1 = "Screen Capture ID1"
const val SCREEN_CAPTURE_CHANNEL_NAME1 = "Screen Capture1"

class MyApp : Application() {
    companion object {
        lateinit var instance: Application
        var ocrEngine: OcrEngine? = null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var image: Bitmap? = null
        var mediaProjection: MediaProjection? = null
        var orientation: Int = 0
        var widthPixels: Int = 0
        var heightPixels: Int = 0
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
        val screenCaptureChannel = NotificationChannel(
            SCREEN_CAPTURE_CHANNEL_ID,
            SCREEN_CAPTURE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(screenCaptureChannel)
    }

    private fun initOCREngine() {
        ocrEngine = OcrEngine(this.applicationContext)
    }

}