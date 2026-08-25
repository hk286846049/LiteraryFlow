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
import android.view.MotionEvent
import android.view.View

data class TextData(
    val text: String,
    val bounds: Rect
) : Parcelable {
    // Parcelable实现保持原有逻辑
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeParcelable(bounds, flags)
    }
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<TextData> {
            override fun createFromParcel(source: Parcel) = TextData(
                source.readString()!!,
                source.readParcelable<Rect>(Rect::class.java.classLoader)!!
            )
            override fun newArray(size: Int) = arrayOfNulls<TextData>(size)
        }
    }
    override fun toString() = "[text='$text', bounds=$bounds]"
}

class TextDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textDataList: List<TextData> = emptyList()
    private var selectedTextData: TextData? = null

    // 绘制工具
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textSize = 12f.spToPx()
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC000000")
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6644CCFF")
    }

    // 交互控制
    var isTextSelectable: Boolean = false
        set(value) {
            if (field != value && !value) {
                selectedTextData = null
                invalidate()
            }
            field = value
        }
    var onTextSelected: ((TextData) -> Unit)? = null

    fun updateTextData(list: List<TextData>) {
        textDataList = list
        selectedTextData = null // 数据更新时清除选中状态
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        textDataList.forEach { data ->
            // 动态选择背景颜色
            val bgColor = if (data === selectedTextData) highlightPaint else bgPaint
            canvas.drawRect(data.bounds, bgColor)

            // 文字绘制逻辑（保持原样）
            val width = data.bounds.width()
            val height = data.bounds.height()
            if (width > 0 && height > 0) {
                val staticLayout = StaticLayout.Builder.obtain(
                    data.text, 0, data.text.length, textPaint, width
                ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()

                canvas.save()
                if (staticLayout.height > height) {
                    canvas.clipRect(data.bounds)
                }
                canvas.translate(data.bounds.left.toFloat(), data.bounds.top.toFloat())
                staticLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isTextSelectable || event.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }

        val x = event.x
        val y = event.y

        // 倒序查找第一个符合条件的元素
        val selected = textDataList.findLast { data ->
            RectF(data.bounds).contains(x, y)
        }

        selected?.let {
            selectedTextData = it
            invalidate()
            onTextSelected?.invoke(it)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun Float.spToPx() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this, resources.displayMetrics
    )
}
