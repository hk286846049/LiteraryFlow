package com.monster.literaryflow.vision.cache

import com.monster.literaryflow.core.model.NormalizedRect
import com.monster.literaryflow.core.model.OcrResultModel

data class ScreenSignature(
    val packageName: String,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val frameSignature: String,
    val roiSignature: String = "FULL"
)

class OcrCache(private val maxEntries: Int = 32) {
    private val values = object : LinkedHashMap<ScreenSignature, OcrResultModel>(
        maxEntries,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ScreenSignature, OcrResultModel>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun get(signature: ScreenSignature): OcrResultModel? = values[signature]

    @Synchronized
    fun put(signature: ScreenSignature, result: OcrResultModel) {
        values[signature] = result
    }

    @Synchronized
    fun clear() = values.clear()

    @Synchronized
    fun size(): Int = values.size
}

object RoiResolver {
    fun fullScreen(): NormalizedRect = NormalizedRect(0f, 0f, 1f, 1f)

    fun topRight(): NormalizedRect = NormalizedRect(0.55f, 0f, 1f, 0.35f)

    fun topLeft(): NormalizedRect = NormalizedRect(0f, 0f, 0.45f, 0.35f)

    fun bottomRight(): NormalizedRect = NormalizedRect(0.55f, 0.65f, 1f, 1f)

    fun bottomLeft(): NormalizedRect = NormalizedRect(0f, 0.65f, 0.45f, 1f)
}
