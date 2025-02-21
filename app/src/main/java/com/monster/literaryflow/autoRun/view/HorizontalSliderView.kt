package com.monster.literaryflow.autoRun.view



import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.monster.literaryflow.R

class HorizontalSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        textAlign = Paint.Align.CENTER
        color = 0xFF000000.toInt() // Black text color
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = 0xFF000000.toInt() // Black border color
    }

    private var buttonPosition = 0f
    private var startText: String = "滑动开始"
    private var endText: String = "完成!"
    private var backgroundDrawable: Drawable? = null
    private var isActivated = false

    private var viewWidth = 0f
    private var viewHeight = 0f

    init {
        if (attrs != null) {
            val a: TypedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.HorizontalSliderView, 0, 0)
            try {
                startText = a.getString(R.styleable.HorizontalSliderView_startText) ?: startText
                endText = a.getString(R.styleable.HorizontalSliderView_endText) ?: endText
                backgroundDrawable = a.getDrawable(R.styleable.HorizontalSliderView_backgroundDrawable)
            } finally {
                a.recycle()
            }
        }

        // Fallback to default drawable if not provided
        if (backgroundDrawable == null) {
            backgroundDrawable = ContextCompat.getDrawable(context, android.R.color.darker_gray)
        }
    }

    fun setStartText(text: String) {
        this.startText = text
        invalidate()
    }

    fun setEndText(text: String) {
        this.endText = text
        invalidate()
    }

    fun setBgDrawable(drawable: Drawable?) {
        this.backgroundDrawable = drawable
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        buttonPosition = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        backgroundDrawable?.let {
            it.setBounds(0, 0, viewWidth.toInt(), viewHeight.toInt())
            it.draw(canvas)
        }

        // Draw text with border
        val text = if (isActivated) endText else startText
        val textX = if (isActivated) buttonPosition - (viewWidth / 2 - 50) else buttonPosition + (viewWidth / 2 - 50)
        val textY = viewHeight / 2 - (paint.descent() + paint.ascent()) / 2

        val textWidth = paint.measureText(text)
        val textBounds = RectF(
            textX - textWidth / 2 - 20, // Left padding
            textY + paint.ascent() - 20, // Top padding
            textX + textWidth / 2 + 20, // Right padding
            textY + paint.descent() + 20 // Bottom padding
        )

        canvas.drawRoundRect(textBounds, 10f, 10f, borderPaint) // Draw border
        canvas.drawText(text, textX, textY, paint) // Draw text
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!isActivated) {
                startAnimation()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, viewWidth - 100f) // Adjusted animation range
        animator.duration = 500
        animator.addUpdateListener {
            buttonPosition = it.animatedValue as Float
            invalidate()
        }
        animator.start()
        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}

            override fun onAnimationEnd(animation: android.animation.Animator) {
                isActivated = true
                invalidate()
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {}

            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }
}

