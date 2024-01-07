package com.monster.literaryflow.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import cn.coderpig.cp_fast_accessibility.back
import cn.coderpig.cp_fast_accessibility.sleep
import cn.coderpig.cp_fast_accessibility.swipe
import com.benjaminwan.ocrlibrary.OcrResult
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.utils.ImageUtils
import com.monster.literaryflow.utils.ScreenUtils
import com.orhanobut.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class CaptureService : Service() {
    private var ocrResult: OcrResult? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, NotificationCompat.Builder(this, SCREEN_CAPTURE_CHANNEL_ID).build())
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
//                MyApp.ocrEngine!!.padding = 100
//                MyApp.ocrEngine!!.unClipRatio = 2.6f
                val hotFlow = MutableSharedFlow<OcrResult>()
                hotFlow.onEach {
                    ocrResult = it
                    Log.d("~~~", "识别时间:${it.detectTime.toInt()}ms")
                    Log.d("~~~", "textBlocks:${it.textBlocks}")
                    MyAccessibilityService.clickNode(it.textBlocks)
                }.launchIn(this)
                while (true) {
                    val image = MyApp.imageReader!!.acquireLatestImage()
                    if (image != null) {
                        try {
                            val bitmap = ImageUtils.imageToBitmap(image)
                            MyApp.image = bitmap
                            val maxSize = max(bitmap.width, bitmap.height)
                            val boxImg: Bitmap = Bitmap.createBitmap(
                                bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
                            )
                            Log.d(
                                "~~~",
                                "selectedImg=${bitmap.height},${bitmap.width} ${bitmap.config}"
                            )
                            val start = System.currentTimeMillis()
                            val ocrResult = MyApp.ocrEngine!!.detect(bitmap, boxImg, maxSize)
                            val end = System.currentTimeMillis()
                            val time = "time=${end - start}ms"
                            hotFlow.emit(ocrResult)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        swipe(500,500,500,800)
                        Log.d("~~~", "image == null")
                    }
                    delay((3000L..4000L).random())
                }
            }

        }
        job.start()

    }

    private fun stopScreenCapture() {
        Log.d("~~~", "stopScreenCapture, virtualDisplay = $MyApp.virtualDisplay")
        MyApp.virtualDisplay!!.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
