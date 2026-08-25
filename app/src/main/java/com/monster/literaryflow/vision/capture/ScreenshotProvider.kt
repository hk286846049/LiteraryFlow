package com.monster.literaryflow.vision.capture

import android.graphics.Bitmap
import com.monster.literaryflow.core.model.NormalizedRect
import com.monster.literaryflow.core.result.FlowResult

interface ScreenshotProvider {
    suspend fun capture(options: ScreenshotOptions = ScreenshotOptions()): FlowResult<ScreenshotFrame>
}

data class ScreenshotOptions(
    val roi: NormalizedRect? = null,
    val maxLongEdge: Int = 1280
)

data class ScreenshotFrame(
    val frameId: Long,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val bitmap: Bitmap
)
