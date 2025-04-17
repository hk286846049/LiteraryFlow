package com.monster.literaryflow.floatWindow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import androidx.core.view.isVisible

class XFloatingWindow(
    private val context: Context,
    private val contentView: View,
    private val config: FloatingWindowConfig = FloatingWindowConfig()
) {
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val layoutParams = LayoutParams().apply { initDefaultParams() }
    private val rootView = FrameLayout(context).apply {
        addView(contentView)
    }
    private val gestureDetector = GestureDetector(context, GestureListener())
    private var isMinimized = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWidth = 0
    private var initialHeight = 0
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { minimize() }
    // 窗口显示/隐藏控制
    fun show() {
        try {
            windowManager.addView(rootView, layoutParams)
            setupTouchListeners()
        } catch (e: Exception) {
            handlePermissionError(e)
        }
    }

    fun hide() {
        windowManager.removeView(rootView)
    }

    // 窗口状态控制
    fun minimize() {
        if (isMinimized) return

        val minimizedSize = dpToPx(40)
        layoutParams.width = minimizedSize
        layoutParams.height = minimizedSize
        contentView.isVisible = false
        updateViewLayout()
        isMinimized = true
    }

    fun expand() {
        if (!isMinimized) return

        layoutParams.width = initialWidth
        layoutParams.height = initialHeight
        contentView.isVisible = true
        updateViewLayout()
        isMinimized = false
    }

    fun toggleMinimize() {
        if (isMinimized) expand() else minimize()
    }
    fun resetAutoHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        if (config.autoHideEnabled) {
            hideHandler.postDelayed(hideRunnable, config.autoHideDelayMillis)
        }
    }

    // 在触摸事件开始时取消自动隐藏

    private fun startAutoHide() {
        if (config.autoHideEnabled && !isMinimized) {
            hideHandler.postDelayed(hideRunnable, config.autoHideDelayMillis)
        }
    }

    private fun cancelAutoHide() {
        hideHandler.removeCallbacks(hideRunnable)
    }


    // 拖动和缩放实现
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListeners() {
        rootView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    cancelAutoHide()
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!config.draggable) return@setOnTouchListener false

                    // 拖动处理
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()

                    // 边缘吸附检测
                    if (config.autoAttachToEdge) {
                        checkEdgeAttach()
                    }

                    updateViewLayout()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    startAutoHide()
                    if (config.autoAttachToEdge) {
                        doEdgeAttach()
                    }
                    true
                }
                else -> false
            }
        }

        // 双指缩放实现
        rootView.setOnGenericMotionListener { _, event ->
            if (event.action == MotionEvent.ACTION_POINTER_DOWN && event.pointerCount == 2) {
                initialWidth = layoutParams.width
                initialHeight = layoutParams.height
                true
            } else if (event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 2 && config.resizable) {
                val width = (initialWidth * event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)).toInt()
                val height = (initialHeight * event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)).toInt()

                layoutParams.width = width.coerceIn(dpToPx(100), getScreenWidth())
                layoutParams.height = height.coerceIn(dpToPx(100), getScreenHeight())
                updateViewLayout()
                true
            } else {
                false
            }
        }
    }

    // 边缘吸附逻辑
    private fun checkEdgeAttach() {
        val screenWidth = getScreenWidth()
        val screenHeight = getScreenHeight()
        val centerX = layoutParams.x + (layoutParams.width / 2)

        if (centerX < screenWidth * 0.25) {
            layoutParams.x = 0
        } else if (centerX > screenWidth * 0.75) {
            layoutParams.x = screenWidth - layoutParams.width
        }
    }

    private fun doEdgeAttach() {
        updateViewLayout()
    }

    // 辅助方法
    private fun updateViewLayout() {
        windowManager.updateViewLayout(rootView, layoutParams)
    }

    private fun LayoutParams.initDefaultParams() {
        width = config.initialWidth
        height = config.initialHeight
        gravity = Gravity.START or Gravity.TOP
        x = config.initialX
        y = config.initialY

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                type = LayoutParams.TYPE_APPLICATION_OVERLAY
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                type = LayoutParams.TYPE_PHONE
            }
            else -> {
                type = LayoutParams.TYPE_SYSTEM_ALERT
            }
        }

        flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                LayoutParams.FLAG_HARDWARE_ACCELERATED

        format = PixelFormat.TRANSLUCENT
    }

    private fun handlePermissionError(e: Exception) {
        // 处理权限错误逻辑
        e.printStackTrace()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun getScreenWidth(): Int {
        return windowManager.defaultDisplay.width
    }

    private fun getScreenHeight(): Int {
        return windowManager.defaultDisplay.height
    }

    // 手势监听器
    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (config.doubleTapToToggle) {
                toggleMinimize()
                return true
            }
            return super.onDoubleTap(e)
        }
    }
}

// 配置类
data class FloatingWindowConfig(
    val initialWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT,
    val initialHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT,
    val initialX: Int = 0,
    val initialY: Int = 0,
    val draggable: Boolean = true,
    val resizable: Boolean = true,
    val autoAttachToEdge: Boolean = true,
    val doubleTapToToggle: Boolean = true,
    val autoHideEnabled: Boolean = true,
    val autoHideDelayMillis: Long = 5000L
)