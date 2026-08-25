package com.monster.literaryflow.vision.ocr

import android.graphics.Bitmap
import com.monster.literaryflow.core.model.OcrResultModel
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.result.FlowResult

interface OcrEngine2 {
    suspend fun recognize(image: Bitmap, options: OcrOptions = OcrOptions()): FlowResult<OcrResultModel>
    fun release()
}

data class OcrOptions(
    val source: RecognitionSource = RecognitionSource.OCR,
    val minConfidence: Float = 0.35f,
    val detectOrientation: Boolean = true
)
