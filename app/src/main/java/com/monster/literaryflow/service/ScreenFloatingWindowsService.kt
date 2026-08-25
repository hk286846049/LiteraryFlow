package com.monster.literaryflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.os.HandlerCompat.postDelayed
import com.monster.fastAccessibility.AccessTouchListener
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.databinding.FloatScreenBinding
import com.monster.literaryflow.rule.ui.ScreenshotOverlayView
import com.monster.literaryflow.rule.ui.TextData
import com.monster.literaryflow.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenFloatingWindowsService : Service() {
    companion object {
        var isRunning = false
        var instance: ScreenFloatingWindowsService? = null
        fun startService(context: Context) {
            if (isRunning) {
                return
            }
            val intent = Intent(context, ScreenFloatingWindowsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var binding: FloatScreenBinding
    private var floatingView: View? = null
    private val CHANNEL_ID = "floating_window_channel"

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addFloatingView()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                createNotification()
            )
        } else {
            startForeground(1, createNotification())
        }
    }
    private var countDownTimer: CountDownTimer? = null

    // 倒计时时长（毫秒）和间隔（毫秒）
    private val totalTime = 6_000L
    private val interval = 1_000L
    private fun addFloatingView() {
        binding = FloatScreenBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root
        binding.screenshotOverlayView.setOnActionListener(object :
            ScreenshotOverlayView.OnActionListener {
            override fun onCoordinatesCaptured(
                left: Float,
                top: Float,
                right: Float,
                bottom: Float
            ) {
                val rect = Rect(
                    left.toInt(),
                    top.toInt(),
                    right.toInt(),
                    bottom.toInt()
                )
                SharedData.rect.postValue(rect)
                CoroutineScope(Dispatchers.Main).launch {
                    AppUtils.openApp(MyApp.instance, "com.monster.literaryflow")
                }
                stopSelf()
            }

            override fun onBitmapCaptured(bitmap: Bitmap) {
                stopSelf()
            }

            override fun onPenetrationStateChange(penetration: Boolean) {
                floatingView?.let { view ->
                    val params = view.layoutParams as WindowManager.LayoutParams
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    binding.tvScreen.visibility = View.VISIBLE
                    startOrResetTimer()
                    windowManager.updateViewLayout(view, params)
                    FastAccessibilityService.instance?.setTouchListener(object :AccessTouchListener{
                        override fun onTouch(): Boolean {
                            startOrResetTimer()
                            return true
                        }
                    })
                }
            }

            override fun onDestroy() {
                CoroutineScope(Dispatchers.Main).launch {
                    AppUtils.openApp(MyApp.instance, "com.monster.literaryflow")
                }
                stopSelf()
            }
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        windowManager.addView(floatingView, params)
    }

    private fun startOrResetTimer() {
        countDownTimer?.cancel()
        // 新建一个倒计时器
        countDownTimer = object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                // 计算剩余秒数（向上取整）
                val secondsLeft = (millisUntilFinished + 999) / 1000
                binding.tvScreen.text = "停止操作${secondsLeft}秒后允许截屏"
            }

            override fun onFinish() {
                FastAccessibilityService.instance?.removeTouchListener()
                val params = floatingView?.layoutParams as WindowManager.LayoutParams
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                binding.screenshotOverlayView.isScreen = true
                windowManager.updateViewLayout(floatingView, params)
                binding.tvScreen.visibility = View.GONE
            }
        }.apply {
            start()
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableArrayListExtra<TextData>("text_data_list")?.let {

        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
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
        countDownTimer?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }


}
