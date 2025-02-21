package com.monster.literaryflow.rule.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*
import kotlin.random.Random

class Firework2View @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 动画参数控制
    private val GRAVITY = 0.98f
    private val GOLDEN_ANGLE = 137.5f
    private val MAX_FIREWORKS = 3
    private val PARTICLES_PER_FIREWORK = 120
    private val PARTICLE_LIFETIME = 2000L
    private val FIREWORK_INTERVAL = 1200L
    private var lastFireworkTime = 0L


    private val particles = mutableListOf<Particle>()
    private val particlePool = mutableListOf<Particle>()
    private val fireworkColors = listOf(
        Color.parseColor("#FFC107"),  // 使用Android标准颜色解析
        Color.parseColor("#F44336"),
        Color.parseColor("#2196F3")
    ).map { color ->
        // 将颜色转换为HSV数组
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv
    }
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = PARTICLE_LIFETIME
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    // 粒子数据结构
    private inner class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var baseHsv: FloatArray = FloatArray(3), // 改为存储HSV值
        var life: Long = 0,
        var startTime: Long = 0,
        var size: Float = 0f,
        var active: Boolean = false
    ) {
        fun reset(x: Float, y: Float, hsv: FloatArray, index: Int) {
            val angleBase = System.currentTimeMillis() % 360
            val speed = 6 + Random.nextFloat() * 4
            // 修正index错误
            val angle = Math.toRadians((angleBase + GOLDEN_ANGLE * index).toDouble())

            this.x = x
            this.y = y
            this.vx = (cos(angle) * speed).toFloat()
            this.vy = (sin(angle) * speed).toFloat() - 8f
            this.baseHsv =  hsv.copyOf()
            this.startTime = System.currentTimeMillis()
            this.size = 8f + Random.nextFloat() * 4f
            this.active = true
        }

        fun update() {
            if (!active) return

            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toFloat() / PARTICLE_LIFETIME

            // 粒子运动物理模拟
            vy += GRAVITY
            x += vx
            y += vy
            vx *= 0.98f // 空气阻力

            // 生命周期结束
            if (progress >= 1f) {
                active = false
                recycleParticle(this)
            }
        }
    }

    // 颜色插值器
    private val colorEvaluator = ArgbEvaluator()
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.size < MAX_FIREWORKS * PARTICLES_PER_FIREWORK) {
            launchFirework()
        }

        particles.removeAll { !it.active }

        particles.forEach { particle ->
            particle.update()
            drawParticle(canvas, particle)
        }
    }

    private fun drawParticle(canvas: Canvas, particle: Particle) {
        val progress = (System.currentTimeMillis() - particle.startTime).toFloat() / PARTICLE_LIFETIME
        val alpha = ((1 - progress) * 255).toInt()
        // 使用初始HSV值进行插值
        val currentHsv = particle.baseHsv.copyOf()
        currentHsv[0] = (currentHsv[0] + progress * 60) % 360  // 色相渐变
        currentHsv[1] = 0.9f - progress * 0.7f               // 饱和度递减

        val color = Color.HSVToColor(alpha, currentHsv)

        // 双层光晕效果
        with(particle) {
            // 主粒子
            haloPaint.color = color
            canvas.drawCircle(x, y, size, haloPaint)

            // 外发光层
            haloPaint.alpha = (alpha * 0.3).toInt()
            canvas.drawCircle(x, y, size * 3, haloPaint)

            // 内发光层
            haloPaint.alpha = (alpha * 0.5).toInt()
            canvas.drawCircle(x, y, size * 1.5f, haloPaint)
        }
    }

    private fun launchFirework() {
        val currentTime = System.currentTimeMillis()
        // 检查时间间隔
        if (currentTime - lastFireworkTime < FIREWORK_INTERVAL) return

        lastFireworkTime = currentTime

        repeat(MAX_FIREWORKS) { fireworkIndex ->
            if (particles.count { it.active } >= MAX_FIREWORKS * PARTICLES_PER_FIREWORK) return

            val color = fireworkColors.random()
            repeat(PARTICLES_PER_FIREWORK) { particleIndex ->
                val particle = obtainParticle().apply {
                    reset(
                        x = width / 2f + Random.nextFloat() * 100 - 50,
                        y = height.toFloat(),
                        hsv = color,
                        index = particleIndex // 添加index传递
                    )
                }
                particles.add(particle)
            }
        }
    }

    // 对象池优化
    private fun obtainParticle(): Particle {
        return particlePool.firstOrNull()?.also { particlePool.remove(it) } ?: Particle()
    }

    private fun recycleParticle(particle: Particle) {
        particlePool.add(particle)
    }

    fun startAnimation() {
        if (!animator.isRunning) {
            animator.start()
        }
    }

    fun stopAnimation() {
        animator.cancel()
    }
}
