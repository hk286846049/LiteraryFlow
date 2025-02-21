package com.monster.literaryflow.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.benjaminwan.ocrlibrary.OcrResult
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.rule.ui.TextData
import com.monster.literaryflow.utils.OcrTextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class CaptureService : Service() {
    private var ocrResult: OcrResult? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var isCollect = true

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(3, NotificationCompat.Builder(this, SCREEN_CAPTURE_CHANNEL_ID).build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setUpVirtualDisplay()
        return START_NOT_STICKY
    }

    private fun setUpVirtualDisplay() {
        val job = GlobalScope.launch {
            withContext(Dispatchers.IO) {
                var continueLoop = true
                while (continueLoop) {
                    if (MyApp.mediaProjection != null) {
                        continueLoop = false
                    }
                }
                MyApp.ocrEngine!!.doAngle = true
                val hotFlow = MutableSharedFlow<OcrResult>()

                hotFlow.onEach { ocrResult1 ->
                    ocrResult = ocrResult1
                    Log.d("#####MONSTER#####", "识别时间:${ocrResult1.detectTime.toInt()}ms")
                    val list: MutableList<TextData> = mutableListOf()
                    if (TextFloatingWindowService.isRunning) {
                        ocrResult1.textBlocks.forEach {
                            val rect = OcrTextUtils.pointsToRect(it.boxPoint)
                            val textData = TextData(it.text, rect)
                            list.add(textData)
                            Log.d(
                                "#####MONSTER#####",
                                "[textBlocks]${it.text}  :${it.boxPoint}"
                            )
                        }
                    }
                    AutoRunManager.updateBitmapNodes(ocrResult1.textBlocks)
                    TextFloatingWindowService.instance?.let { service ->
                        Handler(Looper.getMainLooper()).post {
                            service.updateView(list)
                            isCollect = true
                        }
                    }
                }.launchIn(this)

                while (true) {
                    if (isCollect) {
                        if (TextFloatingWindowService.isRunning) {
                            delay(3000L)
                            TextFloatingWindowService.instance?.let { service ->
                                Handler(Looper.getMainLooper()).post {
                                    service.updateView(mutableListOf())
                                }
                            }
                            delay(300L)
                        }
                        collectImg(hotFlow)
                    }
                }
            }
        }
        job.start()
    }


    private suspend fun collectImg(hotFlow: MutableSharedFlow<OcrResult>) {
        isCollect = false
        try {
            val image = MyApp.imageReader!!.acquireLatestImage()
            if (image != null) {
                val bitmap = ImageUtils.imageToBitmap(image)
                withContext(Dispatchers.Main) {
                    MyApp.image.value = bitmap
                }
                val maxSize = max(bitmap.width, bitmap.height)
                val boxImg: Bitmap = Bitmap.createBitmap(
                    bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
                )
                Log.d(
                    "#####MONSTER#####",
                    "selectedImg=${bitmap.height},${bitmap.width} ${bitmap.config}"
                )

                val ocrResult = MyApp.ocrEngine!!.detect(bitmap, boxImg, maxSize)
                hotFlow.emit(ocrResult)
            } else {
                Log.d("#####MONSTER#####", "image == null")
                delay(1000L)
                collectImg(hotFlow)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            delay(1000L)
            collectImg(hotFlow)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
