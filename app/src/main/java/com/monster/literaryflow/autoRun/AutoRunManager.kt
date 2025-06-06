package com.monster.literaryflow.autoRun

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.benjaminwan.ocrlibrary.OcrResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.Text.TextBlock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.autoRun.autoInterface.IMonitorInterface
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.helper.CaptureManager
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.service.OcrFloatingWindowService
import com.monster.literaryflow.service.TextFloatingWindowService
import com.monster.literaryflow.utils.OcrTextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

object AutoRunManager {
    private val TAG = "【OCR】"
    private val recognizer by lazy {
        Log.d(TAG, "初始化OCR识别引擎")
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun findText(
        text: String,
        findType: TextPickType,
        timeoutSec: Int
    ): Pair<Boolean, TextBlock?> {
        Log.d(
            TAG,
            "开始文本查找任务 | 目标文本: $text | 匹配类型: $findType | 超时时间: ${timeoutSec}秒"
        )
        return try { CaptureManager.withService { service ->
                // 创建独立的协程作用域以便更好地控制取消
                coroutineScope {
                    withTimeoutOrNull(timeoutSec * 1000L) {
                        Log.d(TAG, "进入超时控制流程")
                        processWithRetry(service, text, findType)
                    } ?: run {
                        Log.w(TAG, "查找操作超时")
                        // 取消当前作用域下的所有子协程
                        cancel("查找操作超时")
                        Pair(false, null)
                    }
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "查找操作被取消: ${e.message}")
            Pair(false, null)
        } catch (e: Exception) {
            Log.e(TAG, "findText异常: ${e.message}")
            Pair(false, null)
        }
    }


    private suspend fun processWithRetry(
        service: CaptureService,
        text: String,
        findType: TextPickType,
    ): Pair<Boolean, TextBlock?> {
        var attemptCount = 0
        val retryInterval = 500L // 重试间隔

        while (isActive) {
            attemptCount++
            Log.d(TAG, "开始第${attemptCount}次处理循环")

            try {
                val result = processScreenCapture(service) ?: run {
                    Log.w(TAG, "屏幕捕获失败，尝试第${attemptCount}次重试")
                    delay(retryInterval)
                    return@run null
                }

                findMatchingBlock(result, text, findType)?.let {
                    Log.i(TAG, "找到匹配文本块 | 内容: ${it.text} | 位置: ${it.boundingBox}")
                    return Pair(true, it)
                }

                Log.d(TAG, "当前未找到匹配文本，继续扫描...")
                delay(retryInterval)
            } catch (e: CancellationException) {
                Log.d(TAG, "处理循环被取消")
                throw e // 重新抛出取消异常
            } catch (e: Exception) {
                Log.e(TAG, "处理循环异常: ${e.message}")
                delay(retryInterval)
            }
        }

        Log.d(TAG, "处理循环已终止")
        return Pair(false, null)
    }
    private suspend fun processScreenCapture(service: CaptureService): Text? {
        return try {
            Log.d(TAG, "开始屏幕捕获流程")
            var isGame = false
            val bitmap = service.captureScreen()?.also {
                Log.d(TAG, "成功获取屏幕位图 | 尺寸: ${it.width}x${it.height}")
                if (it.width > it.height) {
                    Log.d(TAG, "检测到游戏界面")
                    isGame = true
                }
            } ?: run {
                Log.w(TAG, "获取屏幕位图失败")
                return null
            }

            val inputImage = InputImage.fromBitmap(bitmap, 0).also {
                Log.d(TAG, "创建ML Kit输入图像完成")
            }
            Log.d(TAG, "启动OCR识别...")
            recognizer.process(inputImage).await()?.also {
                Log.d(TAG, "OCR识别完成 ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "屏幕捕获异常: ${e.stackTraceToString()}")
            null
        }
    }
    private fun detect(img: Bitmap, reSize: Int) :OcrResult{
        MyApp.ocrEngine!!.doAngle = true
        val boxImg: Bitmap = Bitmap.createBitmap(
            img.width, img.height, Bitmap.Config.ARGB_8888
        )
        Log.d(TAG,"selectedImg=${img.height},${img.width} ${img.config}")
        val ocrResult = MyApp.ocrEngine!!.detect(img, boxImg, reSize)
        Log.d(TAG,"识别时间 ${ocrResult.detectTime.toInt()}ms")
        return ocrResult
    }

    private fun findMatchingBlock(
        result: Text?,
        target: String,
        findType: TextPickType
    ): Text.TextBlock? {
        Log.d(TAG, "开始文本匹配 | 模式: $findType")
        return result?.textBlocks?.firstOrNull { block ->
            OcrFloatingWindowService.instance?.let { service ->
                Handler(Looper.getMainLooper()).post {
                    service.updateView(result.textBlocks)
                }
            }

            when (findType) {
                TextPickType.EXACT_MATCH -> {
                    val match = block.text.equals(target, true)
                    Log.v(TAG, "精确匹配检查 | 目标: $target | 当前: ${block.text} | 结果: $match")
                    match
                }
                TextPickType.FUZZY_MATCH -> {
                    val contains = block.text.contains(target, true)
                    Log.v(TAG, "模糊匹配检查 | 目标: $target | 当前: ${block.text} | 结果: $contains")
                    contains
                }
                TextPickType.MULTIPLE_FUZZY_WORDS -> {
                    val keywords = target.split("#")
                    val match = keywords.any { block.text.contains(it, true) }
                    Log.v(TAG, "多关键词匹配 | 目标: $keywords | 当前: ${block.text} | 结果: $match")
                    match
                }
            }
        }?.also {
            Log.d(TAG, "找到符合条件文本块 | 内容: ${it.text} | 位置: ${it.boundingBox}")
        }
    }

    private suspend fun <T> Task<T>.await(): T? = suspendCoroutine { continuation ->
        Log.d(TAG, "开始等待ML Kit任务结果")
        addOnSuccessListener { result ->
            Log.d(TAG, "ML Kit任务成功完成")
            continuation.resume(result)
        }.addOnFailureListener {
            Log.e(TAG, "ML Kit任务失败: ${it.message}")
            continuation.resume(null)
        }
    }

    // 在AutoRunManager对象内新增以下内容
    private val activeMonitors = mutableMapOf<String, Job>()
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

/*
    fun startTextMonitoring(
        targets: Set<TriggerBean>,
        listener: IMonitorInterface
    ): String {
        val monitorId = UUID.randomUUID().toString()

        activeMonitors[monitorId] = monitorScope.launch {
            var lastBitmap: Bitmap? = null
            while (isActive) {
                // 共享捕获结果，避免与主动查找冲突
                val bitmap = CaptureManager.withService { service ->
                    service.captureScreen()?.also { lastBitmap = it }
                } ?: lastBitmap

                bitmap?.let {
                    val textResult = processScreenCapture(it)
                    textResult?.textBlocks?.forEach { block ->
                        // 遍历每个触发条件
                        targets.firstOrNull { triggerBean ->
                            triggerBean.findText?.let { target ->
                                findMatchingBlock(
                                    result = textResult,
                                    target = target,
                                    findType = triggerBean.findTextType
                                ) != null
                            } ?: false
                        }?.let { matchedTrigger ->
                            listener.monitor(matchedTrigger)
                        }
                    }
                }.also {
                    if (it == null) {
                        Log.w(TAG, "位图获取失败，准备重试")
                    }
                }
                delay(1000) // 控制扫描频率
            }
        }

        return monitorId
    }
*/

    fun stopTextMonitoring(monitorId: String) {
        activeMonitors[monitorId]?.cancel()
        activeMonitors.remove(monitorId)
    }

    // 修改现有processScreenCapture支持直接传入bitmap
    private suspend fun processScreenCapture(bitmap: Bitmap): Text? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage).await()
        } catch (e: Exception) {
            Log.e(TAG, "OCR处理异常: ${e.message}")
            null
        }
    }
}
