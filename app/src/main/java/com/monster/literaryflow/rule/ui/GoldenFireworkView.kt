package com.monster.literaryflow.rule.ui

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class GoldenFireworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val GOLDEN_ANGLE = 137.508f // 黄金角度
    }

    private val sparkles = mutableListOf<AdvancedSparkle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gravity = 0.2f
    private var lastLaunchTime = 0L
    private val argbEvaluator = ArgbEvaluator()

    // 烟花爆炸配置
    private data class FireworkConfig(
        val centerX: Float,
        val centerY: Float,
        val baseColor: Int,
        val particleCount: Int = 250,
        val lifeBase: Int = 90
    )

    // 带颜色渐变的粒子
    private inner class AdvancedSparkle(
        position: PointF,
        val startColor: Int,
        val endColor: Int,
        life: Int
    ) {
        var currentPosition = position
        var velocity = PointF(0f, 0f)
        var life = life
        var currentLife = life
        var currentColor = startColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        sparkles.forEach { sparkle ->
            // 颜色渐变计算
            val fraction = 1 - (sparkle.currentLife.toFloat() / sparkle.life)
            sparkle.currentColor = argbEvaluator.evaluate(
                fraction,
                sparkle.startColor,
                sparkle.endColor
            ) as Int

            paint.color = sparkle.currentColor
            paint.alpha = (sparkle.currentLife * 255 / sparkle.life).coerceIn(0, 255)

            canvas.drawCircle(
                sparkle.currentPosition.x,
                sparkle.currentPosition.y,
                6f * fraction, // 大小渐变
                paint
            )
        }

        updateSparkles()
        checkAutoLaunch()
        invalidate()
    }

    private fun checkAutoLaunch() {
        if (System.currentTimeMillis() - lastLaunchTime > 1500) {
            repeat(2 + Random.nextInt(2)) { // 同时发射 2-3 个烟花
                launchFirework()
            }
            lastLaunchTime = System.currentTimeMillis()
        }
    }

    private fun launchFirework() {
        // 随机生成3个爆炸中心点
        repeat(3) { i ->
            postDelayed({
                createGoldenExplosion(
                    FireworkConfig(
                        centerX = width * 0.2f + Random.nextFloat() * width * 0.6f,
                        centerY = height * 0.3f + Random.nextFloat() * height * 0.4f,
                        baseColor = generateRandomColor(),
                        particleCount = 180
                    )
                )
            }, i * 200L)
        }
    }

    private fun generateRandomColor(): Int {
        return Color.HSVToColor(floatArrayOf(
            Random.nextFloat() * 360,
            0.8f,
            1f
        ))
    }

    // 黄金角度扩散生成
    private fun createGoldenExplosion(config: FireworkConfig) {
        var currentAngle = 0f

        repeat(config.particleCount) {
            // 使用黄金角度增量
            currentAngle = (currentAngle + GOLDEN_ANGLE) % 360
            val radians = Math.toRadians(currentAngle.toDouble())

            val speedBase = 4f + Random.nextFloat() * 8f
            val velocity = PointF(
                (cos(radians) * speedBase).toFloat(),
                (sin(radians) * speedBase).toFloat() * -1f
            )

            // 生成颜色渐变组合
            val endColor = Color.argb(
                100,
                Color.red(config.baseColor),
                Color.green(config.baseColor),
                Color.blue(config.baseColor)
            )

            sparkles.add(
                AdvancedSparkle(
                    position = PointF(config.centerX, config.centerY),
                    startColor = Color.WHITE,  // 起始颜色为白色
                    endColor = endColor,
                    life = config.lifeBase + Random.nextInt(30)
                ).apply {
                    this.velocity = velocity
                }
            )

            // 添加重力随机变化
            velocity.y += gravity * (0.8f + Random.nextFloat() * 0.4f)
        }
    }

    private fun updateSparkles() {
        val iterator = sparkles.iterator()
        while (iterator.hasNext()) {
            val sparkle = iterator.next()
            sparkle.currentPosition.x += sparkle.velocity.x
            sparkle.currentPosition.y += sparkle.velocity.y
            sparkle.velocity.x *= 0.98f  // 空气阻力
            sparkle.velocity.y *= 0.98f
            sparkle.currentLife--

            if (sparkle.currentLife <= 0) iterator.remove()
        }
    }

    fun startAnimation() {
        invalidate()
    }
}
