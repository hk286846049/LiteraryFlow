package com.monster.literaryflow.core.model

data class ScreenMetrics(
    val width: Int,
    val height: Int,
    val rotation: Int = 0,
    val insetLeft: Int = 0,
    val insetTop: Int = 0,
    val insetRight: Int = 0,
    val insetBottom: Int = 0
)

class CoordinateTransformer {
    fun toScreen(point: NormalizedPoint, metrics: ScreenMetrics): PixelPoint {
        val contentWidth = (metrics.width - metrics.insetLeft - metrics.insetRight).coerceAtLeast(1)
        val contentHeight = (metrics.height - metrics.insetTop - metrics.insetBottom).coerceAtLeast(1)
        val x = (point.x.coerceIn(0f, 1f) * contentWidth).toInt() + metrics.insetLeft
        val y = (point.y.coerceIn(0f, 1f) * contentHeight).toInt() + metrics.insetTop
        return PixelPoint(x.coerceIn(0, metrics.width - 1), y.coerceIn(0, metrics.height - 1))
    }

    fun toNormalized(point: PixelPoint, metrics: ScreenMetrics): NormalizedPoint {
        val contentWidth = (metrics.width - metrics.insetLeft - metrics.insetRight).coerceAtLeast(1)
        val contentHeight = (metrics.height - metrics.insetTop - metrics.insetBottom).coerceAtLeast(1)
        return NormalizedPoint(
            x = (point.x - metrics.insetLeft) / contentWidth.toFloat(),
            y = (point.y - metrics.insetTop) / contentHeight.toFloat()
        ).normalized()
    }
}

data class NormalizedPoint(val x: Float, val y: Float) {
    fun normalized(): NormalizedPoint = NormalizedPoint(
        x.coerceIn(0f, 1f),
        y.coerceIn(0f, 1f)
    )
}

data class PixelPoint(val x: Int, val y: Int)
