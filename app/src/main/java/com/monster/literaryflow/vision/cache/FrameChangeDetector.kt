package com.monster.literaryflow.vision.cache

import android.graphics.Bitmap
import com.monster.literaryflow.core.model.NormalizedRect

interface FrameChangeDetector {
    fun isChanged(previous: Bitmap?, current: Bitmap, roi: NormalizedRect? = null): Boolean
}

class AverageHashFrameChangeDetector(
    private val threshold: Int = 8
) : FrameChangeDetector {
    override fun isChanged(previous: Bitmap?, current: Bitmap, roi: NormalizedRect?): Boolean {
        if (previous == null) return true
        val left = roi ?: RoiResolver.fullScreen()
        val previousHash = hash(previous, left)
        val currentHash = hash(current, left)
        return hammingDistance(previousHash, currentHash) >= threshold
    }

    private fun hash(bitmap: Bitmap, roi: NormalizedRect): Long {
        val left = (bitmap.width * roi.left).toInt().coerceIn(0, bitmap.width - 1)
        val top = (bitmap.height * roi.top).toInt().coerceIn(0, bitmap.height - 1)
        val right = (bitmap.width * roi.right).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bitmap.height * roi.bottom).toInt().coerceIn(top + 1, bitmap.height)
        val pixels = IntArray(64)
        var total = 0L
        var index = 0
        for (row in 0 until 8) {
            for (column in 0 until 8) {
                val x = left + ((right - left) * column / 8)
                val y = top + ((bottom - top) * row / 8)
                val pixel = bitmap.getPixel(x.coerceAtMost(right - 1), y.coerceAtMost(bottom - 1))
                val gray = ((pixel shr 16 and 0xff) * 299 +
                    (pixel shr 8 and 0xff) * 587 +
                    (pixel and 0xff) * 114) / 1000
                pixels[index++] = gray
                total += gray
            }
        }
        val average = total / pixels.size
        var hash = 0L
        pixels.forEachIndexed { i, value ->
            if (value >= average) hash = hash or (1L shl i)
        }
        return hash
    }

    private fun hammingDistance(first: Long, second: Long): Int {
        return java.lang.Long.bitCount(first xor second)
    }
}
