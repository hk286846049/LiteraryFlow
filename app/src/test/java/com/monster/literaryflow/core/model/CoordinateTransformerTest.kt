package com.monster.literaryflow.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateTransformerTest {
    @Test
    fun transformsWithInsetsAndClamps() {
        val metrics = ScreenMetrics(width = 1000, height = 2000, insetTop = 100, insetBottom = 100)
        val transformer = CoordinateTransformer()
        val pixel = transformer.toScreen(NormalizedPoint(0.5f, 0.5f), metrics)
        assertEquals(PixelPoint(500, 1000), pixel)
        assertEquals(NormalizedPoint(0.5f, 0.5f), transformer.toNormalized(pixel, metrics))
    }
}
