package com.monster.literaryflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.text.Text
import com.monster.literaryflow.R
import com.monster.literaryflow.databinding.OcrFloatingWindowBinding
import com.monster.literaryflow.databinding.TextFloatingWindowBinding
import com.monster.literaryflow.rule.ui.TextData

class OcrFloatingWindowService : Service() {
    companion object {
        var isRunning = false
        var instance: OcrFloatingWindowService? = null
    }

    private lateinit var windowManager: WindowManager
    private lateinit var binding: OcrFloatingWindowBinding
    private var floatingView: View? = null
    private val CHANNEL_ID = "ocr_window_channel"

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addFloatingView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, createNotification())
        }
    }

    private fun addFloatingView() {
        binding = OcrFloatingWindowBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        windowManager.addView(floatingView, params)
        floatingView?.setOnTouchListener { _, _ ->
            binding.textDisplayView.updateTextData(emptyList())
            false
        }
    }

    fun updateView(list: List<Text.TextBlock>?) {
        if (list != null) {
            val textDataList = list.map { textBlock ->
                val text = textBlock.text
                val adjustedBounds = Rect(
                    textBlock.boundingBox?.left!!,
                    textBlock.boundingBox?.top!! - getStatusBarHeight(),
                    textBlock.boundingBox?.right!!,
                    textBlock.boundingBox?.bottom!! - getStatusBarHeight()
                )
                TextData(text, adjustedBounds)
            }

            binding.textDisplayView.updateTextData(textDataList)
        }else{
            binding.textDisplayView.updateTextData(emptyList())
        }

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableArrayListExtra<TextData>("text_data_list")?.let {
//            updateView(it)
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ocr Window Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮窗服务")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(R.drawable.icon_bg)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        isRunning = false
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}
