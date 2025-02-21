package com.monster.literaryflow.rule.ui

import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View

data class TextData(
    val text: String,
    val bounds: Rect
) : Parcelable {
    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        TODO("Not yet implemented")
    }
}
class TextDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textDataList: List<TextData> = emptyList()

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textSize = 12f.spToPx()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66000000") // 半透明黑色背景
    }

    /**
     * 更新数据并刷新视图
     */
    fun updateTextData(list: List<TextData>) {
        textDataList = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 清除旧内容
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        Log.d("TextDisplayView", "text size ${textDataList.size}")

        textDataList.forEach { data ->
            val width = data.bounds.width()
            val height = data.bounds.height()

            // 绘制背景矩形
            canvas.drawRect(data.bounds, bgPaint)

            if (width > 0 && height > 0) {
                val staticLayout = StaticLayout.Builder.obtain(
                    data.text,
                    0,
                    data.text.length,
                    textPaint,
                    width
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()

                // **计算 `StaticLayout` 的高度**，如果超出 `bounds` 则进行裁剪
                val textHeight = staticLayout.height
                if (textHeight > height) {
                    canvas.save()
                    canvas.clipRect(data.bounds) // **裁剪区域，防止超出**
                    canvas.translate(data.bounds.left.toFloat(), data.bounds.top.toFloat())
                    staticLayout.draw(canvas)
                    canvas.restore()
                } else {
                    // 直接绘制，不需要裁剪
                    canvas.save()
                    canvas.translate(data.bounds.left.toFloat(), data.bounds.top.toFloat())
                    staticLayout.draw(canvas)
                    canvas.restore()
                }
            }
        }
    }

    // SP转PX扩展函数
    private fun Float.spToPx(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        resources.displayMetrics
    )
}

