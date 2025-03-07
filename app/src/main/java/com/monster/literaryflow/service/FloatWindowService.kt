package com.monster.literaryflow.service

import CustomTask
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
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
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import cn.coderpig.cp_fast_accessibility.NodeWrapper
import cn.coderpig.cp_fast_accessibility.back
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.startSliding
import cn.coderpig.cp_fast_accessibility.swipe
import com.benjaminwan.ocrlibrary.OcrResult
import com.benjaminwan.ocrlibrary.TextBlock
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.R
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.autoRun.adapter.FloatAdapter
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.databinding.FloatingWindowBinding
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.room.AppDatabase
import com.monster.literaryflow.room.AutoInfoDao
import com.monster.literaryflow.service.FloatingWindowService.Companion.toastTip
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.OcrTextUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class FloatingWindowService : Service() {
    companion object {
        var toastTip: MutableLiveData<String>? = null
    }

    private lateinit var binding: FloatingWindowBinding
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var autoInfoDao: AutoInfoDao? = null
    private lateinit var adapter: FloatAdapter
    private lateinit var task: CustomTask
    private var list: List<AutoInfo>? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initView()
        Glide.with(this)
            .load(getDrawable(R.drawable.hashiq))
            .transform(CircleCrop())
            .into(binding.logo)
        binding.logo.setOnClickListener {
            binding.layoutLogo.visibility = View.GONE
            binding.logo.visibility = View.GONE
            binding.layout.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                updateAdapter()
            }
        }
        toastTip?.value = ""
        toastTip?.observeForever {
            binding.tvTip.text = it
        }
        val database = AppDatabase.getDatabase(this)
        autoInfoDao = database.autoInfoDao()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.mRecyclerView.layoutManager = layoutManager
        task = CustomTask(autoInfoDao!!)
        adapter = FloatAdapter(mutableListOf()) {
            Log.d("FloatService", "${it.runAppName} : ${it.isRun}")
            CoroutineScope(Dispatchers.IO).launch {
                if (autoInfoDao != null) {
                    autoInfoDao!!.update(it)
                    if (binding.switchOpen.isChecked) {
                        withContext(Dispatchers.Main) {
                            binding.logo.visibility = View.VISIBLE
                            binding.layoutLogo.visibility = View.VISIBLE
                            binding.layout.visibility = View.GONE
                        }
                        task.cancel()
                        delay(100)
                        task.start()
                        task.sendData(adapter!!.getList())

                    }
                } else {
                    Log.e("FloatService", "autoInfoDao is null!")
                }
            }
        }

        binding.mRecyclerView.adapter = adapter
        CoroutineScope(Dispatchers.IO).launch {
            list = autoInfoDao?.getAll()
                ?.filter { it.runState && it.runTimes > it.todayRunTime.second }
            withContext(Dispatchers.Main) {
                list?.let { adapter.updateList(it) }
            }
        }
        binding.switchOpen.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                task.start()
                task.sendData(adapter.getList())
            } else {
                task.cancel()
            }
        }
        MyApp.isUpdateData.observeForever {
            if (it) {
                CoroutineScope(Dispatchers.IO).launch {
                    updateAdapter()
                }
                MyApp.isUpdateData.value = false
            }
        }
        binding.ivClose.setOnClickListener {
            task.cancel()
            stopSelf()
        }
        startForegroundServiceProperly()

    }

    private fun startForegroundServiceProperly() {
        // 1. 创建通知渠道（仅 Android 8.0+ 需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_window_channel",
                "Floating Window",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating Window Service Channel"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        // 2. 构建兼容性通知
        val notification = NotificationCompat.Builder(this, "floating_window_channel")
            .setContentTitle("Service Running")
            .setContentText("Keeping your service active")
            .setSmallIcon(R.drawable.hashiq)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // 3. 启动前台服务
        try {
            startForeground(2, notification)
        } catch (e: Exception) {
            // 处理可能的异常（如 Android 12+ 的后台限制）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startForeground(
                    2,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(2, notification)
            }

        }
    }

    private suspend fun updateAdapter() {
        val list = autoInfoDao?.getAll()?.filter { it.runState }
        if (list != null) {
            withContext(Dispatchers.Main) {
                adapter.updateList(list)
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        binding = FloatingWindowBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root
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
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 0

        val edgeThreshold = 240 // 靠边吸附的阈值
        val screenWidth = resources.displayMetrics.widthPixels

        // 添加悬浮窗
        windowManager.addView(floatingView, layoutParams)

        // GestureDetector 处理点击
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (binding.logo.visibility == View.VISIBLE) {
                        // 点击 logo 显示完整布局
                        binding.layoutLogo.visibility = View.GONE
                        binding.logo.visibility = View.GONE
                        binding.layout.visibility = View.VISIBLE
                    }
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    // 长按时不做特殊处理，拖拽由 onTouch 处理
                }
            })

        // 处理拖拽逻辑
        val dragTouchListener = object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
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

                        // 防止误判点击（只有移动超过一定距离才算拖拽）
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                        }

                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            // 吸附逻辑
                            if (layoutParams.x < edgeThreshold) {
                                layoutParams.x = 0 // 吸附到左侧
                                collapseToEdge()
                            } else if (layoutParams.x > screenWidth - floatingView!!.width - edgeThreshold) {
                                layoutParams.x = screenWidth - floatingView!!.width // 吸附到右侧
                                collapseToEdge()
                            }
                            windowManager.updateViewLayout(floatingView, layoutParams)
                        }
                        return true
                    }
                }
                return false
            }
        }

        // **核心改进点：给 `logo` 绑定单独的触摸事件**
        binding.logo.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true // 让 GestureDetector 处理点击
            }

            // 如果是拖拽，则执行拖拽逻辑
            dragTouchListener.onTouch(v, event)
        }

        floatingView!!.setOnTouchListener(dragTouchListener)
    }

    /**
     * 处理靠边时的 UI 逻辑
     */
    private fun collapseToEdge() {
        binding.layout.visibility = View.GONE
        binding.layoutLogo.visibility = View.VISIBLE
        binding.logo.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }

}

