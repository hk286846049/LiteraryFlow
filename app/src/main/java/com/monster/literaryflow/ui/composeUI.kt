package com.monster.literaryflow.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class composeUI {
}

/**
 * 模仿 CSS 动效的加载动画
 *
 * 参数：
 * - [spinnerSize]：容器尺寸（默认 45dp，与 CSS 中 --uib-size 一致）
 * - [spinnerColor]：圆点颜色
 * - [animationDuration]：周期，单位 ms（默认 2500ms，即 2.5s）
 */
@Composable
fun OrbitSpinner(
    spinnerSize: Dp = 45.dp,
    spinnerColor: Color = Color.Black,
    animationDuration: Int = 2500
) {
    // 此处可以安全地调用 rememberInfiniteTransition() 和其他 Composable API
    val infiniteTransition = rememberInfiniteTransition()
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = TweenSpec(durationMillis = animationDuration, easing = LinearEasing)
        )
    )

    // Canvas 尺寸固定为 spinnerSize
    Canvas(modifier = Modifier.size(spinnerSize)) {
        // 本例中容器为正方形，取最小边长作为动画基准
        val spinnerSizePx = size.minDimension
        // 每个切片高度（容器分为 6 份）
        val sliceHeight = spinnerSizePx / 6f
        // 水平基准中心
        val baseCenterX = spinnerSizePx / 2f
        // 根据 CSS，伪元素尺寸为容器宽度的 1/6，
        // 则圆点半径基准值为 (spinnerSizePx/6)/2 = spinnerSizePx/12
        val baseRadius = spinnerSizePx / 12f

        // 全局无限动画（0～1 循环，周期 animationDuration）

        // 定义关键帧数据，与 CSS 中的 keyframes 对应
        data class OrbitKeyframe(val fraction: Float, val transFactor: Float, val scale: Float, val alpha: Float)
        val keyframes = listOf(
            OrbitKeyframe(0f,    0.25f,    0.73684f, 0.65f),
            OrbitKeyframe(0.05f,  0.235f,   0.684208f, 0.58f),
            OrbitKeyframe(0.10f,  0.182f,   0.631576f, 0.51f),
            OrbitKeyframe(0.15f,  0.129f,   0.578944f, 0.44f),
            OrbitKeyframe(0.20f,  0.076f,   0.526312f, 0.37f),
            OrbitKeyframe(0.25f,  0f,       0.47368f,  0.3f),
            OrbitKeyframe(0.30f, -0.076f,   0.526312f, 0.37f),
            OrbitKeyframe(0.35f, -0.129f,   0.578944f, 0.44f),
            OrbitKeyframe(0.40f, -0.182f,   0.631576f, 0.51f),
            OrbitKeyframe(0.45f, -0.235f,   0.684208f, 0.58f),
            OrbitKeyframe(0.50f, -0.25f,    0.73684f,  0.65f),
            OrbitKeyframe(0.55f, -0.235f,   0.789472f, 0.72f),
            OrbitKeyframe(0.60f, -0.182f,   0.842104f, 0.79f),
            OrbitKeyframe(0.65f, -0.129f,   0.894736f, 0.86f),
            OrbitKeyframe(0.70f, -0.076f,   0.947368f, 0.93f),
            OrbitKeyframe(0.75f,  0f,       1f,        1f),
            OrbitKeyframe(0.80f,  0.076f,   0.947368f, 0.93f),
            OrbitKeyframe(0.85f,  0.129f,   0.894736f, 0.86f),
            OrbitKeyframe(0.90f,  0.182f,   0.842104f, 0.79f),
            OrbitKeyframe(0.95f,  0.235f,   0.789472f, 0.72f),
            OrbitKeyframe(1f,     0.25f,    0.73684f,  0.65f)
        )

        // 根据归一化时间 t（0～1）返回水平位移（单位：px）、缩放因子、透明度
        fun getOrbitState(t: Float): Triple<Float, Float, Float> {
            // 查找 t 所在的区间
            var prev = keyframes[0]
            var next = keyframes[0]
            for (kf in keyframes) {
                if (kf.fraction <= t) {
                    prev = kf
                }
                if (kf.fraction >= t) {
                    next = kf
                    break
                }
            }
            // 如果 t 正好等于某个关键帧
            if (prev.fraction == next.fraction) {
                return Triple(prev.transFactor * spinnerSizePx, prev.scale, prev.alpha)
            }
            val interval = next.fraction - prev.fraction
            val localT = (t - prev.fraction) / interval
            val transFactor = prev.transFactor + localT * (next.transFactor - prev.transFactor)
            val scale = prev.scale + localT * (next.scale - prev.scale)
            val alpha = prev.alpha + localT * (next.alpha - prev.alpha)
            return Triple(transFactor * spinnerSizePx, scale, alpha)
        }

        // 定义每个切片中两个圆点的动画起始偏移（归一化值）
        // 根据 CSS 中各伪元素的 animation-delay 换算得到：
        // （注意：负延时在无限动画中等价于动画的相位偏移）
        val sliceOffsets = listOf(
            Pair(0f,       0.5f),       // 第一“切片”：默认 (::before, ::after)
            Pair(0.83333f, 0.33333f),    // 第二“切片”
            Pair(0.66667f, 0.16667f),    // 第三“切片”
            Pair(0.5f,     0f),          // 第四“切片”
            Pair(0.33333f, 0.83333f),    // 第五“切片”
            Pair(0.16667f, 0.66667f)     // 第六“切片”
        )

        // 按切片顺序绘制各圆点
        sliceOffsets.forEachIndexed { index, offsets ->
            // 当前切片垂直中心（每个切片高度为 spinnerSizePx/6）
            val sliceCenterY = (index + 0.5f) * sliceHeight
            // 对当前切片内的两个圆点（模拟 ::before 和 ::after）
            listOf(offsets.first, offsets.second).forEach { offset ->
                // 计算当前圆点的“归一化动画时间”
                // 注意：用 ((progress + offset) mod 1) 保证结果在 [0,1] 范围内
                val effectiveT = ((progress + offset) % 1f + 1f) % 1f
                val (transX, scale, alpha) = getOrbitState(effectiveT)
                // 最终圆点中心位置：基于容器水平中心加上动画计算的水平偏移，
                // 垂直位置为当前切片的中心
                val cx = baseCenterX + transX
                val cy = sliceCenterY
                // 圆点半径基准为 baseRadius，乘以动画缩放因子
                val radius = baseRadius * scale
                drawCircle(
                    color = spinnerColor.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}