package com.monster.literaryflow.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.benjaminwan.ocrlibrary.OcrResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognition.*
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.helper.CaptureManager
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.rule.ui.TextData
import com.monster.literaryflow.utils.OcrTextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max



class CaptureService : Service() {
    private val TAG = "CaptureService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var imageReader: ImageReader

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建初始化")
        CaptureManager.registerService(this)
        setupForeground()
        initImageReader()
    }

    private fun setupForeground() {
        Log.d(TAG, "启动前台服务")
        startForeground(3, NotificationCompat.Builder(this, SCREEN_CAPTURE_CHANNEL_ID).build())
    }

    private fun initImageReader() {
        val metrics = resources.displayMetrics
        Log.d(TAG, "初始化ImageReader | 分辨率: ${metrics.widthPixels}x${metrics.heightPixels}")
        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        ).also {
            MyApp.imageReader = it
            Log.d(TAG, "ImageReader配置完成")
        }
    }

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(MyApp.imageReader!!) { // 添加同步锁
            try {
                val image = MyApp.imageReader!!.acquireLatestImage()
                Log.d(TAG, "成功获取图像 | 格式: ${image.format} | 时间戳: ${image.timestamp}")
                ImageUtils.imageToBitmap(image).also {
                    Log.d(TAG, "图像转换位图完成")
                    image.close()
                    Log.d(TAG, "已释放图像资源")
                }
            } catch (e: Exception) {
                Log.e(TAG, "捕获屏幕异常: ${e.stackTraceToString()}")
                null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "服务销毁清理")
        serviceScope.cancel()
        imageReader.close()
        Log.d(TAG, "资源释放完成")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "绑定服务请求")
        return null
    }
}



