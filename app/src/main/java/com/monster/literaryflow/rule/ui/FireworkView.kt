package com.monster.literaryflow.rule.ui

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FireworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 烟花粒子数据结构
    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Int,
        var life: Int
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint()
    private val handler = Handler(Looper.getMainLooper())
    private val gravity = 0.2f

    private val colors = listOf(
        Color.RED, Color.YELLOW, Color.CYAN,
        Color.MAGENTA, Color.GREEN, Color.WHITE
    )

    private val runnable = object : Runnable {
        override fun run() {
            createFirework()
            handler.postDelayed(this, 1000) // 每秒发射一次烟花
        }
    }

    private fun createFirework() {
        // 初始化烟花参数
        val startX = width / 2f + Random.nextFloat() * width / 4f
        val startY = height.toFloat()

        // 创建爆炸粒子
        repeat(150) {
            val angle = (it * 2.4).toFloat()
            val speed = 5 + Random.nextFloat() * 8
            particles.add(Particle(
                x = startX,
                y = startY,
                vx = speed * cos(angle),
                vy = -speed * sin(angle),
                color = colors.random(),
                life = 60 + Random.nextInt(30)
            ))
        }
    }

    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += gravity
            p.life--

            if (p.life <= 0) iterator.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK) // 设置黑色背景

        particles.forEach { p ->
            paint.color = p.color
            paint.alpha = (p.life / 100f * 255).toInt()
            canvas.drawCircle(p.x, p.y, 4f, paint)
        }

        updateParticles()
        invalidate()
    }

    fun startAnimation() {
        handler.post(runnable)
    }

    fun stopAnimation() {
        handler.removeCallbacks(runnable)
    }
}
