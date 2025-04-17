package com.monster.literaryflow.service

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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import cn.coderpig.cp_fast_accessibility.NodeWrapper
import cn.coderpig.cp_fast_accessibility.back
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.startSliding
import cn.coderpig.cp_fast_accessibility.stopSliding
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
import com.monster.literaryflow.autoRun.task.CustomTask
import com.monster.literaryflow.autoRun.task.ScheduledTask
import com.monster.literaryflow.autoRun.task.TaskSchedule
import com.monster.literaryflow.autoRun.task.TaskScheduler
import com.monster.literaryflow.autoRun.view.AutoListActivity
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.AutoRunType
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
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class FloatingWindowService : Service() {
    companion object {
        var toastTip: MutableLiveData<String>? = null
        var isCreate = false
        var instance: FloatingWindowService? = null
    }

    private lateinit var bottomStatusView: View
    private lateinit var bottomLayoutParams: WindowManager.LayoutParams
    private var isBottomWindowShowing = false

    private lateinit var binding: FloatingWindowBinding
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var autoInfoDao: AutoInfoDao? = null
    private lateinit var adapter: FloatAdapter
    private lateinit var task: CustomTask
    private var list: List<AutoInfo>? = null
    private var isRunning = false


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isCreate = true
        instance = this
        Log.i("悬浮窗服务", "========== 服务启动 ==========")
        initView()
        Glide.with(this)
            .load(getDrawable(R.drawable.hashiq))
            .transform(CircleCrop())
            .into(binding.logo)
        binding.logo.setOnClickListener {
            Log.d("悬浮窗UI", "点击Logo，展开主界面")
            binding.layoutLogo.visibility = View.GONE
            binding.logo.visibility = View.GONE
            binding.layout.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                updateAdapter()
            }
        }
        toastTip?.value = ""
        toastTip?.observeForever {
            // 处理Toast消息

        }
        val database = AppDatabase.getDatabase(this)
        autoInfoDao = database.autoInfoDao()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.mRecyclerView.layoutManager = layoutManager
        binding.mRecyclerView.setOnLongClickListener {
            //进入AutoList页面
            val intent = Intent(this, AutoListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            true
        }

        task = CustomTask(this, autoInfoDao!!).apply {
            onStatusUpdate = { taskName, loopType, progress, total ->
                if (progress > 0) {
                    updateTaskStatus(taskName, loopType, progress, total)
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        bottomStatusView.visibility = View.GONE
                    }
                }
            }
        }
        adapter = FloatAdapter(mutableListOf()) {
            Log.i("任务管理", "任务状态变更 -> 应用:${it.runAppName} | 状态:${it.isRun}")
            CoroutineScope(Dispatchers.IO).launch {
                if (autoInfoDao != null) {
                    autoInfoDao!!.update(it)
                    if (isRunning) {
                        withContext(Dispatchers.Main) {
                            binding.logo.visibility = View.VISIBLE
                            binding.layoutLogo.visibility = View.VISIBLE
                            binding.layout.visibility = View.GONE
                        }
                        task.cancel()
                        delay(100)
                        task.start()
                        task.sendData(adapter.getList())
                    }
                } else {
                    Log.e("数据库错误", "autoInfoDao为空!")
                }
            }
        }

        binding.mRecyclerView.adapter = adapter
        CoroutineScope(Dispatchers.IO).launch {
            list = autoInfoDao?.getAll()?.filter { it.runState }
            withContext(Dispatchers.Main) {
                list?.let { adapter.updateList(it) }
            }
        }
        binding.ivRun.setOnClickListener {
            if (!isRunning) {
                binding.ivRun.setImageResource(R.drawable.float_pause)
                task.start()
                task.sendData(adapter.getList())
                isRunning = true
            } else {
                isRunning = false
                binding.ivRun.setImageResource(R.drawable.float_run)
                task.cancel()
            }
        }

        binding.ivHide.setOnClickListener {
            Log.i("悬浮窗服务", "隐藏悬浮窗")
            binding.outLayout.visibility = View.INVISIBLE
        }

        MyApp.isUpdateData.observeForever {
            if (it) {
                Log.d("数据更新", "接收到数据更新通知")
                CoroutineScope(Dispatchers.IO).launch {
                    updateAdapter()
                }
                MyApp.isUpdateData.value = false
            }
        }
        binding.ivClose.setOnClickListener {
            Log.i("悬浮窗服务", "用户点击关闭按钮，停止服务")
            task.cancel()
            stopSelf()
        }
        binding.ivBack.setOnClickListener {
            binding.layoutLogo.visibility = View.VISIBLE
            binding.logo.visibility = View.VISIBLE
            binding.layout.visibility = View.GONE
        }
        startForegroundServiceProperly()
        initBottomStatusWindow()

    }

    private fun initBottomStatusWindow() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bottomStatusView = inflater.inflate(R.layout.bottom_status_window, null)

        bottomLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        // 添加底部悬浮窗
        windowManager.addView(bottomStatusView, bottomLayoutParams)
        isBottomWindowShowing = true

        // 初始隐藏，当有任务时显示
        bottomStatusView.visibility = View.GONE
    }

    private fun updateTaskStatus(
        taskName: String,
        loopType: AutoRunType?,
        progress: Int,
        total: Int
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            var title = ""
            when (loopType) {
                AutoRunType.LOOP -> {
                    bottomStatusView.findViewById<ProgressBar>(R.id.progress_task).apply {
                        max = 1
                        this.progress = progress
                    }
                    title = "[循环]"
                    bottomStatusView.findViewById<TextView>(R.id.tv_progress).text =
                        "$progress/无限次"
                }

                AutoRunType.DAY_LOOP -> {
                    bottomStatusView.findViewById<ProgressBar>(R.id.progress_task).apply {
                        max = total
                        this.progress = progress
                    }
                    bottomStatusView.findViewById<TextView>(R.id.tv_progress).text =
                        "$progress/$total"

                    title = "[每日定时]"
                }

                AutoRunType.SPECIFIED_NUMBER -> {
                    bottomStatusView.findViewById<ProgressBar>(R.id.progress_task).apply {
                        max = total
                        this.progress = progress
                    }
                    bottomStatusView.findViewById<TextView>(R.id.tv_progress).text =
                        "$progress/$total"

                    title = "[固定次数]"

                }

                AutoRunType.WEEK_LOOP -> {
                    bottomStatusView.findViewById<ProgressBar>(R.id.progress_task).apply {
                        max = total
                        this.progress = progress
                    }
                    bottomStatusView.findViewById<TextView>(R.id.tv_progress).text =
                        "$progress/$total"
                    title = "[每周定时]"
                }

                else -> {
                    bottomStatusView.findViewById<ProgressBar>(R.id.progress_task).apply {
                        max = 0
                        this.progress = 0
                    }
                    bottomStatusView.findViewById<TextView>(R.id.tv_progress).text = ""

                }
            }
            bottomStatusView.findViewById<TextView>(R.id.tv_current_task).text =
                "当前任务：$title$taskName"

            if (bottomStatusView.visibility != View.VISIBLE) {
                bottomStatusView.visibility = View.VISIBLE
            }
        }
    }

    private fun startForegroundServiceProperly() {
        Log.d("前台服务", "正在启动前台服务...")
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

        val notification = NotificationCompat.Builder(this, "floating_window_channel")
            .setContentTitle("Service Running")
            .setContentText("Keeping your service active")
            .setSmallIcon(R.drawable.hashiq)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    2,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(2, notification)
            }
            Log.i("前台服务", "前台服务启动成功")
        } catch (e: Exception) {
            Log.e("前台服务", "启动前台服务失败", e)
        }
    }

    fun showView() {
        binding.outLayout.visibility = View.VISIBLE
    }

    private suspend fun updateAdapter() {
        Log.d("数据加载", "正在更新任务列表数据...")
        val list = autoInfoDao?.getAll()?.filter { it.runState }
        if (list != null) {
            withContext(Dispatchers.Main) {
                adapter.updateList(list)
                Log.d("数据加载", "任务列表更新完成，共${list.size}条数据")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        Log.d("悬浮窗UI", "正在初始化悬浮窗视图...")
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

        val edgeThreshold = 240
        val screenWidth = resources.displayMetrics.widthPixels

        windowManager.addView(floatingView, layoutParams)
        Log.d("悬浮窗UI", "悬浮窗视图添加成功")

        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (binding.logo.visibility == View.VISIBLE) {
                        Log.d("手势检测", "检测到单击事件，展开主界面")
                        binding.layoutLogo.visibility = View.GONE
                        binding.logo.visibility = View.GONE
                        binding.layout.visibility = View.VISIBLE
                    }
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    Log.d("手势检测", "检测到长按事件")
                }
            })

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

                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                            Log.d("悬浮窗拖动", "正在拖动悬浮窗，偏移量X:$deltaX, Y:$deltaY")
                        }

                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            Log.d("悬浮窗拖动", "拖动结束，处理吸附逻辑")
                            if (layoutParams.x < edgeThreshold) {
                                layoutParams.x = 0
                                collapseToEdge()
                                Log.d("悬浮窗吸附", "吸附到左侧")
                            } else if (layoutParams.x > screenWidth - floatingView!!.width - edgeThreshold) {
                                layoutParams.x = screenWidth - floatingView!!.width
                                collapseToEdge()
                                Log.d("悬浮窗吸附", "吸附到右侧")
                            }
                            windowManager.updateViewLayout(floatingView, layoutParams)
                        }
                        return true
                    }
                }
                return false
            }
        }

        binding.logo.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            dragTouchListener.onTouch(v, event)
        }

        floatingView!!.setOnTouchListener(dragTouchListener)
    }

    private fun collapseToEdge() {
        Log.d("悬浮窗UI", "折叠到边缘状态")
        binding.layout.visibility = View.GONE
        binding.layoutLogo.visibility = View.VISIBLE
        binding.logo.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        Log.i("悬浮窗服务", "========== 服务销毁 ==========")
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        if (isBottomWindowShowing) {
            windowManager.removeView(bottomStatusView)
            isBottomWindowShowing = false
        }
        isCreate = false
        isRunning = false
        instance = null
    }
}

object SharedData {
    val trigger = MutableLiveData<String>()
}