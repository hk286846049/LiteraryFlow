package com.monster.literaryflow.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.monster.fastAccessibility.AccessTouchListener
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.databinding.FloatTextNavigationBinding
import com.monster.literaryflow.databinding.TextFloatingWindowBinding
import com.monster.literaryflow.rule.ui.TextData
import com.monster.literaryflow.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TextFloatingWindowService : Service() {
    companion object {
        var isRunning = false
        var instance: TextFloatingWindowService? = null
        fun startService(context: Context) {
            val intent = Intent(context, TextFloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var binding: TextFloatingWindowBinding
    private var floatingView: View? = null
    private var navigationView: View? = null
    private val CHANNEL_ID = "floating_window_channel"
    private var isGetText = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addFloatingView()
        addNavigationView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                createNotification()
            )
        } else {
            startForeground(1, createNotification())
        }
        CoroutineScope(Dispatchers.IO).launch {
            (FastAccessibilityService.instance as? MyAccessibilityService)?.startFetchingNodes(
                interval = 200L,findText = null, timeoutMillis = null, isUpdateWindow = true
            )
        }

    }


    private fun addFloatingView() {
        binding = TextFloatingWindowBinding.inflate(LayoutInflater.from(this))
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
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNavigationView() {
        var selectText: TextData? = null
        val binding = FloatTextNavigationBinding.inflate(LayoutInflater.from(this))
        navigationView = binding.root

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    Log.d("手势检测", "检测到长按事件")
                }
            })

        val dragTouchListener = object : View.OnTouchListener {
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(event)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()

                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                            Log.d("悬浮窗拖动", "正在拖动悬浮窗，偏移量X:$deltaX, Y:$deltaY")
                        }

                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        windowManager.updateViewLayout(navigationView, layoutParams)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            windowManager.updateViewLayout(navigationView, layoutParams)
                        }
                        return true
                    }
                }
                return false
            }
        }

        binding.layout.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            dragTouchListener.onTouch(v, event)
        }

        windowManager.addView(navigationView, layoutParams)

        binding.tvGetCoords.setOnClickListener {
            binding.actionGroup.visibility = View.GONE
            if (isGetText) {
                binding.tvGetCoords.text = "开始选择"
                isGetText = false
                floatingView?.let { view ->
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

                    windowManager.updateViewLayout(view, params)
                    this.binding.textDisplayView.isTextSelectable = false
                }
            } else {
                binding.tvGetCoords.text = "暂停选择"
                isGetText = true
                floatingView?.let { view ->
                    val params = view.layoutParams as WindowManager.LayoutParams
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    windowManager.updateViewLayout(view, params)
                    this.binding.textDisplayView.isTextSelectable = true
                    this.binding.textDisplayView.onTextSelected = {
                        Log.d("TextFloatingWindowService", "onTextSelected: $it")
                        selectText = it
                        binding.actionGroup.visibility = View.VISIBLE
                        navigationView?.post {
                            val layoutParams = navigationView?.layoutParams as? WindowManager.LayoutParams
                            layoutParams?.let { params ->
                                // 计算文字中心点坐标
                                val centerX = it.bounds.centerX()
                                val textBottom = it.bounds.bottom

                                // 获取navigationView高度
                                val navHeight = navigationView?.height ?: 0

                                // 设置新位置（在文字下方）
                                params.x = centerX - (navigationView?.width ?: 0) / 2
                                params.y = textBottom + 10  // 加10像素间距

                                windowManager.updateViewLayout(navigationView, params)
                            }
                        }
                    }
                }


            }
        }
        binding.tvText.setOnClickListener {
            selectText?.let {
                SharedData.textRect.postValue(it.text)
                CoroutineScope(Dispatchers.Main).launch {
                    AppUtils.openApp(MyApp.instance, "com.monster.literaryflow")
                }
                stopSelf()
            }
        }
        binding.tvLocation.setOnClickListener {
            selectText?.let {
                SharedData.rect.postValue(it.bounds)
                CoroutineScope(Dispatchers.Main).launch {
                    AppUtils.openApp(MyApp.instance, "com.monster.literaryflow")
                }
                stopSelf()
            }
        }
        binding.tvCancel.setOnClickListener {
            stopSelf()
        }
    }
    fun updateView(list: List<TextData>) {
        if (isGetText) {
            return
        }
        val adjustedList = list.map { data ->
            val adjustedBounds = Rect(
                data.bounds.left,
                data.bounds.top - getStatusBarHeight(),
                data.bounds.right,
                data.bounds.bottom - getStatusBarHeight()
            )
            TextData(data.text, adjustedBounds)
        }
        binding.textDisplayView.updateTextData(adjustedList)
        binding.textDisplayView.isTextSelectable = true
        binding.textDisplayView.onTextSelected = {
            Log.d("TextFloatingWindowService", "onTextSelected: $it")
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableArrayListExtra<TextData>("text_data_list")?.let {
            updateView(it)
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
        navigationView?.let { windowManager.removeView(it) }
        isRunning = false
        instance = null
        isGetText = false
        (FastAccessibilityService.instance as? MyAccessibilityService)?.stopAllFetchingNodes()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}
