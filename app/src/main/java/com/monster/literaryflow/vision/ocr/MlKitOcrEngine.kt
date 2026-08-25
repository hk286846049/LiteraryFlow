package com.monster.literaryflow.vision.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.monster.literaryflow.core.model.IntRect
import com.monster.literaryflow.core.model.NormalizedRect
import com.monster.literaryflow.core.model.OcrBlockModel
import com.monster.literaryflow.core.model.OcrResultModel
import com.monster.literaryflow.core.result.ErrorCode
import com.monster.literaryflow.core.result.FlowError
import com.monster.literaryflow.core.result.FlowResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlKitOcrEngine : OcrEngine2 {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override suspend fun recognize(image: Bitmap, options: OcrOptions): FlowResult<OcrResultModel> {
        return try {
            val text = recognizer.process(InputImage.fromBitmap(image, 0)).await()
            val result = OcrResultModel(
                frameId = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                width = image.width,
                height = image.height,
                rotation = 0,
                blocks = text.textBlocks.mapNotNull { it.toBlock(image.width, image.height) }
                    .filter { it.confidence >= options.minConfidence || it.confidence == 0f }
            )
            FlowResult.Success(result)
        } catch (error: Exception) {
            FlowResult.Failure(FlowError(ErrorCode.OCR_FAILED, error.message ?: "OCR 失败", error))
        }
    }

    override fun release() {
        recognizer.close()
    }

    private fun Text.TextBlock.toBlock(width: Int, height: Int): OcrBlockModel? {
        val box = boundingBox ?: return null
        return OcrBlockModel(
            text = text,
            confidence = 0f,
            boundingBox = box.toIntRect(),
            normalizedBox = box.toNormalizedRect(width, height)
        )
    }

    private fun Rect.toIntRect(): IntRect = IntRect(left, top, right, bottom)

    private fun Rect.toNormalizedRect(width: Int, height: Int): NormalizedRect {
        return NormalizedRect(
            left = left / width.toFloat(),
            top = top / height.toFloat(),
            right = right / width.toFloat(),
            bottom = bottom / height.toFloat()
        ).normalized()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
}
