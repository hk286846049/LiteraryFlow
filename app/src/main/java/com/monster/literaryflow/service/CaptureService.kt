package com.monster.literaryflow.service

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
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
import com.monster.literaryflow.R
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.helper.CaptureManager
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.rule.ui.TextData
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.OcrTextUtils
import com.monster.literaryflow.utils.ScreenUtils
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


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建初始化")
        CaptureManager.registerService(this)
        val notification = NotificationCompat.Builder(this, SCREEN_CAPTURE_CHANNEL_ID)
            .setContentTitle("屏幕捕获进行中")
            .setContentText("您的屏幕正在被捕获")
            .setSmallIcon(R.drawable.logo)
            .build()
        startForeground(3, notification)
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            // 在这里处理 MediaProjection 停止时的逻辑，比如释放虚拟显示器等资源
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            mediaProjection!!.registerCallback(mediaProjectionCallback, Handler(Looper.getMainLooper()))
            val findHorText = intent.getBooleanExtra("findHorText", false)

            MyApp.virtualDisplay = if (findHorText) {
                mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    ScreenUtils.getScreenHeight(this),
                    ScreenUtils.getScreenWidth(),
                    ScreenUtils.getScreenDensityDpi(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    MyApp.imageReader?.surface, null, null
                )
            } else {
                mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    ScreenUtils.getScreenWidth(),
                    ScreenUtils.getScreenHeight(this),
                    ScreenUtils.getScreenDensityDpi(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    MyApp.imageReader?.surface,
                    null,
                    null
                )
            }
            MyApp.mediaProjection = mediaProjection
        }

        return START_STICKY
    }

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        val imageReader = MyApp.imageReader ?: run {
            Log.e(TAG, "ImageReader is null")
            return@withContext null
        }

        synchronized(imageReader) {
            try {
                Log.d(TAG, "尝试获取图像")
                val image = imageReader.acquireLatestImage() ?: run {
                    Log.d(TAG, "没有可用的新图像")
                    return@synchronized null
                }

                try {
                    Log.d(TAG, "成功获取图像 | 格式: ${image.format} | 时间戳: ${image.timestamp}")
                    return@synchronized ImageUtils.imageToBitmap(image).also {
                        Log.d(TAG, "图像转换位图完成")
                    }
                } finally {
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
        MyApp.imageReader?.close()
        MyApp.imageReader = null
        Log.d(TAG, "资源释放完成")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "绑定服务请求")
        return null
    }
}



