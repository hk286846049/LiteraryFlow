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
import cn.coderpig.cp_fast_accessibility.sleep
import com.benjaminwan.ocrlibrary.OcrResult
import com.monster.literaryflow.Const.Companion.CqPackage
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.utils.ScreenUtils
import com.monster.literaryflow.utils.TimeUtils
import com.orhanobut.logger.Logger
import io.gate.gateapi.ApiClient
import io.gate.gateapi.ApiException
import io.gate.gateapi.Configuration
import io.gate.gateapi.GateApiException
import io.gate.gateapi.api.AccountApi
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Calendar
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
                initGateIo()
                var continueLoop = true
                while (continueLoop) {
                    if (MyApp.mediaProjection != null) {
                        continueLoop = false
                    }
                }
                MyApp.ocrEngine!!.doAngle = true
                val hotFlow = MutableSharedFlow<OcrResult>()
                hotFlow.onEach {
                    ocrResult = it
                    Log.d("~~~", "识别时间:${it.detectTime.toInt()}ms")
                    Log.d("~~~", "textBlocks:${it.textBlocks}")
                    MyAccessibilityService.clickNode(it.textBlocks)
                    Log.d("~~~", "$it")
                }.launchIn(this)
                while (true) {
                    /*  when(MyApp.runRuleApp){
                          CqPackage ->{
                              if (TimeUtils.isCurrentTime(12,0)){

                              }else  if (TimeUtils.isCurrentTime(19,30)){

                              }else  if (TimeUtils.isCurrentTime(20,0)){

                              }else  if (TimeUtils.isCurrentTime(21,0)){

                              }
                          }
                          else ->{
                              collectImg(hotFlow)
                          }
                      }*/
                    var hour = 0
                    var minute = 0

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val currentTime = LocalTime.now()
                        hour = currentTime.hour
                        minute = currentTime.minute
                    } else {
                        val calendar = Calendar.getInstance()
                        hour = calendar.get(Calendar.HOUR_OF_DAY)
                        minute = calendar.get(Calendar.MINUTE)
                    }
                  /*  if ((hour == 12 && minute in 0..15) || (hour == 7 && minute in 30..45) || (hour == 8 && minute in 0..15) || (hour == 9 && minute in 0..35)) {
                        collectImg(hotFlow)
                    }*/
                    collectImg(hotFlow)

                    delay((2000L..3000L).random())
                }
            }
        }
        job.start()
    }

    //api交易
    //https://github.com/gateio/gateapi-java
    //https://www.gate.io/zh/myaccount/api_key_manage
    private fun initGateIo(){
        val defaultClient: ApiClient = Configuration.getDefaultApiClient()
        defaultClient.setBasePath("https://api.gateio.ws/api/v4")
        defaultClient.setApiKeySecret("dc3a65828c1638222cf79f8cda058b05", "859ccc4a8fc9e46d9342d646a1da32ce9512f7e91204fb50aa6e790bf8f84e6f")
        val apiInstance = AccountApi(defaultClient)
        try {
            val result = apiInstance.accountDetail
            println(result)
        } catch (e: GateApiException) {
            System.err.println(
                String.format(
                    "Gate api exception, label: %s, message: %s",
                    e.errorLabel,
                    e.message
                )
            )
            e.printStackTrace()
        } catch (e: ApiException) {
            System.err.println("Exception when calling AccountApi#getAccountDetail")
            System.err.println("Status code: " + e.code)
            System.err.println("Response headers: " + e.responseHeaders)
            e.printStackTrace()
        }
    }

    private val connectionMutex = Mutex()

    private suspend fun goHuodong(time: Int) {
        connectionMutex.withLock {
        }
    }

    private suspend fun collectImg(hotFlow: MutableSharedFlow<OcrResult>) {
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
            Log.d("~~~", "image == null")
        }
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
