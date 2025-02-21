package com.monster.literaryflow.rule.ui

import android.graphics.*
import android.view.animation.LinearInterpolator
import kotlin.math.*
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import kotlin.math.*
import kotlin.random.Random

class AdvancedFireworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val activeFireworks = mutableListOf<Rocket>()
    private val sparkles = mutableListOf<Sparkle>()
    private val paint = Paint().apply { isAntiAlias = true }
    private val handler = Handler(Looper.getMainLooper())
    private val accelerometer = FloatArray(2)

    // 属性动画相关
    private val animators = mutableListOf<ValueAnimator>()
    private val gravity = 0.25f

    // 火箭数据结构
    private inner class Rocket(
        var x: Float,
        var y: Float,
        val targetY: Float,
        val color: Int
    ) {
        var currentY = y
        var radius = 6f
    }

    // 爆炸火花数据结构（使用属性动画）
    private inner class Sparkle(
        startX: Float,
        startY: Float,
        val color: Int,
        val life: Int = 60 + Random.nextInt(30)
    ) {
        var position = PointF(startX, startY)
        var velocity = PointF(
            (Random.nextFloat() - 0.5f) * 15f,
            -abs(Random.nextFloat() * 30f)
        )
        var alpha = 255
        var scale = 0.6f + Random.nextFloat() * 0.4f // 初始尺寸更小
    }


    private val runnable = object : Runnable {
        override fun run() {
            launchRocket()
            handler.postDelayed(this, 4500)
        }
    }

    private fun launchRocket() {
        // 创建升空弹道
        val rocket = Rocket(
            // [初始横向位置] width*0.2f保证最小起始位置，width*0.6f控制横向随机范围
            x = width * 0.2f + Random.nextFloat() * width * 0.6f,
            y = height.toFloat(),
            // [爆炸高度] height*0.3f是底部保留高度，height*0.4f控制爆炸区域纵向范围
            targetY = height * 0.3f + Random.nextFloat() * height * 0.4f,
            color = Color.argb(255, 255, 100 + Random.nextInt(155), 0)
        )

        // 使用属性动画控制升空
        val animator = ValueAnimator.ofFloat(height.toFloat(), rocket.targetY).apply {
            // [升空持续时间] 基础800ms + 随机400ms，值越大上升速度越慢
            duration = (800 + Random.nextInt(400)).toLong()
            // [运动曲线] DecelerateInterpolator(1.5f)实现先快后慢效果
            // 参数1.5f控制减速曲线形状，值越大开始速度越快
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animation ->
                rocket.currentY = animation.animatedValue as Float
                // 6f基础半径 + 根据动画进度衰减的10f尾迹长度
                rocket.radius = 6f + (1 - animation.animatedFraction) * 10f  // 火箭尾迹效果
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    explodeFirework(rocket)
                    activeFireworks.remove(rocket)
                }
            })
            start()
        }

        activeFireworks.add(rocket)
        animators.add(animator)
    }

    private fun explodeFirework(rocket: Rocket) {
        // 创建爆炸粒子（使用物理模拟+属性动画）
        val explosionColors = intArrayOf(
            Color.RED, Color.YELLOW, rocket.color,
            Color.WHITE, Color.CYAN, Color.MAGENTA
        )

        // 增加粒子密度
        val particleCount = 100 + Random.nextInt(150)  // [总粒子数] 基础100 + 随机150（变化范围100-250）

        // 改进的粒子分布算法（使用极坐标+随机偏移）
        val coreParticles = particleCount / 4  // [核心密度区] 保证中心至少有25%的高密度粒子
        repeat(coreParticles + particleCount) {
            // [速度分层] 核心区域4-12f速度，外围8-24f速度（更动态的扩散效果）
            val baseSpeed = when {
                it < coreParticles -> 4f + Random.nextFloat() * 8f  // 中间区域更慢
                else -> 8f + Random.nextFloat() * 16f
            }

            val angle = Random.nextFloat() * 2 * PI.toFloat() // 随机角度
            val speedVariation = if (it % 2 == 0) 1f else 0.7f // 速度变化
            // [位置随机偏移] 通过offset产生立体分布效果
            val offset = Random.nextFloat() * 0.4f - 0.2f

            // 极坐标转笛卡尔坐标
            val sparkle = Sparkle(
                // [起始位置计算] cos/sin生成环形分布，offset*30f控制散布半径
                startX = rocket.x + cos(angle) * offset * 30f,
                startY = rocket.currentY + sin(angle) * offset * 30f,
                color = explosionColors.random()
            ).apply {
                // [运动方向] angle+offset产生螺旋效果，-6f给初始上升动力
                velocity.x = cos(angle + offset) * baseSpeed * speedVariation
                velocity.y = sin(angle + offset) * baseSpeed * speedVariation - 6f
            }

            // 添加粒子缩放动画改进
            sparkle.run {
                ValueAnimator.ofFloat(0.8f, 1.7f).apply {  // 缩小缩放幅度
                    duration = 1200L
                    interpolator = LinearInterpolator()
                    // 调整缩放值计算方式
                    addUpdateListener { animation ->
                        scale =
                            (animation.animatedValue as Float) * (0.9f + Random.nextFloat() * 0.2f)
                    }
                }.start()
            }
            sparkles.add(sparkle)
        }
    }

    private fun updateSparkles() {
        val iterator = sparkles.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            s.position.x += s.velocity.x
            s.position.y += s.velocity.y
            s.velocity.y += gravity * 0.5f
            if (s.alpha <= 0) iterator.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        // 绘制升空弹道
        activeFireworks.forEach { rocket ->
            paint.color = rocket.color
            canvas.drawCircle(rocket.x, rocket.currentY, rocket.radius, paint)
        }

        // 绘制爆炸粒子
        sparkles.forEach { s ->
            paint.color = s.color
            paint.alpha = s.alpha
            // [动态半径公式]
            // - 3f基础半径 + scale缩放系数*2f（控制粒子大小范围）
            // - (alpha/255f)使粒子随透明度衰减尺寸，实现「渐隐缩小」效果
            // 实际半径范围：最小3*0.6*0=0  最大(3+1*2)*1=5f（原固定6f）
            // 最终效果：更细腻的尺寸变化，透明度越高时尺寸越大
            val radius = (4f + s.scale * 2f) * (s.alpha / 255f)  // 动态半径+透明度衰减
            canvas.drawCircle(s.position.x, s.position.y, radius, paint)
        }

        updateSparkles()
        invalidate()
    }

    fun startShow() {
        handler.post(runnable)
    }

    fun stopShow() {
        handler.removeCallbacks(runnable)
        animators.forEach { it.cancel() }
        animators.clear()
    }
}

