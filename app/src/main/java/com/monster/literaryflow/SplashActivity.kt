package com.monster.literaryflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                // 使用改进后的柯基绘制作为启动页
                CorgiSplashScreen(onAnimationComplete = {
                    showSplash = false
                })
            } else {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
        }
    }
}

@Composable
fun CorgiSplashScreen(onAnimationComplete: () -> Unit) {
    // 采用缩放动画使柯基图形从缩小逐渐放大
    val scaleAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        )
        onAnimationComplete()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2

        // 以较小边长作为基准，并结合动画缩放比例
        val baseScale = min(canvasWidth, canvasHeight) / 300f * scaleAnim.value

        // ----------------------
        // 绘制柯基身体（采用圆角矩形）
        // ----------------------
        val bodyWidth = 180f * baseScale
        val bodyHeight = 100f * baseScale
        val bodyLeft = centerX - bodyWidth / 2
        val bodyTop = centerY
        drawRoundRect(
            color = Color(0xFFFFA726), // 橙黄色调
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(50f * baseScale, 50f * baseScale)
        )

        // ----------------------
        // 绘制柯基头部（圆形）
        // ----------------------
        val headRadius = 50f * baseScale
        val headCenter = Offset(centerX, centerY - headRadius)
        drawCircle(
            color = Color(0xFFFFA726),
            radius = headRadius,
            center = headCenter
        )

        // ----------------------
        // 绘制耳朵（利用 Path 绘制三角形）
        // ----------------------
        // 左耳
        val leftEarPath = Path().apply {
            moveTo(headCenter.x - headRadius * 0.6f, headCenter.y - headRadius * 0.8f)
            lineTo(headCenter.x - headRadius * 1.2f, headCenter.y - headRadius * 1.8f)
            lineTo(headCenter.x - headRadius * 0.2f, headCenter.y - headRadius * 1.4f)
            close()
        }
        drawPath(leftEarPath, color = Color(0xFFFFA726))
        // 右耳（对称绘制）
        val rightEarPath = Path().apply {
            moveTo(headCenter.x + headRadius * 0.6f, headCenter.y - headRadius * 0.8f)
            lineTo(headCenter.x + headRadius * 1.2f, headCenter.y - headRadius * 1.8f)
            lineTo(headCenter.x + headRadius * 0.2f, headCenter.y - headRadius * 1.4f)
            close()
        }
        drawPath(rightEarPath, color = Color(0xFFFFA726))

        // ----------------------
        // 绘制脸部白色斑块
        // ----------------------
        drawCircle(
            color = Color.White,
            radius = headRadius * 0.7f,
            center = headCenter
        )

        // ----------------------
        // 绘制眼睛
        // ----------------------
        val eyeRadius = 5f * baseScale
        drawCircle(
            color = Color.Black,
            radius = eyeRadius,
            center = Offset(headCenter.x - 15f * baseScale, headCenter.y - 10f * baseScale)
        )
        drawCircle(
            color = Color.Black,
            radius = eyeRadius,
            center = Offset(headCenter.x + 15f * baseScale, headCenter.y - 10f * baseScale)
        )

        // ----------------------
        // 绘制鼻子（椭圆）
        // ----------------------
        val noseWidth = 10f * baseScale
        val noseHeight = 6f * baseScale
        drawOval(
            color = Color.Black,
            topLeft = Offset(headCenter.x - noseWidth, headCenter.y + 5f * baseScale),
            size = Size(noseWidth * 2, noseHeight * 2)
        )

        // ----------------------
        // 绘制前后腿
        // ----------------------
        val legWidth = 15f * baseScale
        val legHeight = 30f * baseScale
        // 前腿（左右各一）
        drawRoundRect(
            color = Color(0xFFFFA726),
            topLeft = Offset(centerX - bodyWidth / 3 - legWidth / 2, bodyTop + bodyHeight),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(5f * baseScale, 5f * baseScale)
        )
        drawRoundRect(
            color = Color(0xFFFFA726),
            topLeft = Offset(centerX + bodyWidth / 3 - legWidth / 2, bodyTop + bodyHeight),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(5f * baseScale, 5f * baseScale)
        )
        // 后腿（略靠身体中部）
        drawRoundRect(
            color = Color(0xFFFFA726),
            topLeft = Offset(centerX - bodyWidth / 2 + legWidth, bodyTop + bodyHeight * 0.5f),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(5f * baseScale, 5f * baseScale)
        )
        drawRoundRect(
            color = Color(0xFFFFA726),
            topLeft = Offset(centerX + bodyWidth / 2 - legWidth * 2, bodyTop + bodyHeight * 0.5f),
            size = Size(legWidth, legHeight),
            cornerRadius = CornerRadius(5f * baseScale, 5f * baseScale)
        )

        // ----------------------
        // 绘制尾巴（简单的弧线）
        // ----------------------
        val tailPath = Path().apply {
            moveTo(centerX + bodyWidth / 2, bodyTop + bodyHeight * 0.3f)
            quadraticBezierTo(
                centerX + bodyWidth / 2 + 20f * baseScale,
                bodyTop + bodyHeight * 0.2f,
                centerX + bodyWidth / 2 + 10f * baseScale,
                bodyTop + bodyHeight * 0.1f
            )
        }
        drawPath(
            path = tailPath,
            color = Color(0xFFFFA726),
            style = Stroke(width = 4f * baseScale, cap = StrokeCap.Round)
        )
    }
}
