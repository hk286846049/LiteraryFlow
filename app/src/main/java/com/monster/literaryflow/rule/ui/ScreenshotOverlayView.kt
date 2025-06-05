package com.monster.literaryflow.rule.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.monster.literaryflow.R
import kotlin.math.max
import kotlin.math.min

class ScreenshotOverlayView(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isSelecting = false
    private var screenBitmap: Bitmap? = null // 预存的屏幕截图
    private var onActionListener: OnActionListener? = null

    private val statusBarHeight: Int by lazy {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }
    var isScreen: Boolean = true
        set(value) {
            field = value
            // 控制 View 是否可交互
            isClickable = value
            isFocusable = value
            invalidate()
        }

    // 设置监听器
    fun setOnActionListener(listener: OnActionListener) {
        this.onActionListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isScreen) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 清空之前的选区
                selectedRect = null
                startX = event.rawX
                startY = event.rawY - statusBarHeight // 补偿状态栏高度
                currentX = startX
                currentY = startY
                isSelecting = true
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentX = event.rawX
                currentY = event.rawY
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                isSelecting = false
                selectedRect = RectF(
                    min(startX, currentX),
                    min(startY, currentY),
                    max(startX, currentX),
                    max(startY, currentY)
                )
                showActionButtons()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private var selectedRect: RectF? = null

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isScreen) {
            val rectToDraw = if (isSelecting) {
                RectF(
                    min(startX, currentX),
                    min(startY, currentY),
                    max(startX, currentX),
                    max(startY, currentY)
                )
            } else {
                selectedRect
            }
            rectToDraw?.let { rect ->
                // 绘制填充区域
                val paint = Paint().apply {
                    color = Color.argb(50, 0, 120, 215)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rect, paint)

                // 绘制边框
                paint.color = Color.BLUE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawRect(rect, paint)
            }

        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showActionButtons() {
        // 显示操作按钮（示例使用PopupWindow）
        val popupView = LayoutInflater.from(context).inflate(R.layout.screen_popup_actions, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        // 计算按钮显示位置（右下角偏移）
        val x = currentX.toInt()
        val y = (currentY + statusBarHeight).toInt() // 恢复状态栏偏移
        popupWindow.showAtLocation(this, Gravity.NO_GRAVITY, x, y)

        // 按钮点击处理
        popupView.findViewById<TextView>(R.id.btn_get_coords).setOnClickListener {
            selectedRect = null
            val left = min(startX, currentX)
            val top = min(startY, currentY) +statusBarHeight
            val right = max(startX, currentX)
            val bottom = max(startY, currentY)+statusBarHeight
            onActionListener?.onCoordinatesCaptured(left, top, right, bottom)
            Toast.makeText(
                context,
                "top:$top,bottom:$bottom,left:$left,right:$right",
                Toast.LENGTH_SHORT
            ).show()
            invalidate()
            popupWindow.dismiss()
        }
        popupView.findViewById<TextView>(R.id.btn_save).setOnClickListener {
            selectedRect = null
            screenBitmap?.let { bitmap ->
                val croppedBitmap = cropBitmap(bitmap, startX, startY, currentX, currentY)
                onActionListener?.onBitmapCaptured(croppedBitmap)
            }
            invalidate()
            popupWindow.dismiss()
        }
        popupView.findViewById<TextView>(R.id.btn_reset).setOnClickListener {
            selectedRect = null
            invalidate()
            popupWindow.dismiss()
        }
        popupView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            selectedRect = null
            invalidate()
            popupWindow.dismiss()
            onActionListener?.onDestroy()
        }
        popupView.findViewById<TextView>(R.id.btn_pause).setOnClickListener {
            selectedRect = null
            if (isScreen) {
                isScreen = false
                (it as TextView).text = "开始截屏"
                popupWindow.dismiss()
            } else {
                isScreen = true
                (it as TextView).text = "暂停截屏"
                popupWindow.dismiss()
            }
            onActionListener?.onPenetrationStateChange(!isScreen)
            invalidate()
        }
    }

    private fun cropBitmap(
        bitmap: Bitmap,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Bitmap {
        val left = min(startX, endX).coerceAtLeast(0f)
        val top = min(startY, endY).coerceAtLeast(0f)
        val right = max(startX, endX).coerceAtMost(bitmap.width.toFloat())
        val bottom = max(startY, endY).coerceAtMost(bitmap.height.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            left.toInt(),
            top.toInt(),
            (right - left).toInt(),
            (bottom - top).toInt()
        )
    }

    interface OnActionListener {
        fun onCoordinatesCaptured(left: Float, top: Float, right: Float, bottom: Float)
        fun onBitmapCaptured(bitmap: Bitmap)
        fun onPenetrationStateChange(penetration: Boolean)
        fun onDestroy()
    }
}
