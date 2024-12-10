package com.monster.literaryflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.benjaminwan.ocrlibrary.OcrEngine
import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.monster.fastAccessibility.Const.Companion.multipleX
import com.monster.fastAccessibility.Const.Companion.multipleY
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.utils.ScreenUtils
import io.gate.gateapi.ApiClient
import io.gate.gateapi.ApiException
import io.gate.gateapi.Configuration
import io.gate.gateapi.GateApiException
import io.gate.gateapi.api.AccountApi
import io.gate.gateapi.api.FuturesApi
import io.gate.gateapi.api.WalletApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher


const val SCREEN_CAPTURE_CHANNEL_ID = "Screen Capture ID"
const val SCREEN_CAPTURE_CHANNEL_NAME = "Screen Capture"
const val SCREEN_CAPTURE_CHANNEL_ID1 = "Screen Capture ID1"
const val SCREEN_CAPTURE_CHANNEL_NAME1 = "Screen Capture1"

class MyApp : Application() {
    companion object {
        lateinit var instance: Application
        var ocrEngine: OcrEngine? = null
//        var binanceApi : SpotClient?=null
        var binanceFuturesApi : UMFuturesClientImpl?=null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var image: Bitmap? = null
        var mediaProjection: MediaProjection? = null
        var orientation: Int = 0
        var widthPixels: Int = 0
        var heightPixels: Int = 0
        //当前运行app
        var runRuleApp:String ?=null
        // 开光状态
        var isRun:Boolean = false
        var isMj = true
        var isJd = true
        var isKf = true
        var isMh = true
        var accountApi :AccountApi? = null
        var futuresApi : FuturesApi? = null
        var walletApi  : WalletApi? = null
        val amount = 0
        var SOLA = 6L
        var SOLB = 2L
        var WIF = 200L
        var FET = 400L
        var INJ = 75L
        var op = 300L
        var APT = 300L
        var PEPE = 1L
        var canBuyNum = 0L
        var buyOrderId = 0L
        var sellOrderId = 0L
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FastAccessibilityService.init(
            instance, MyAccessibilityService::class.java, arrayListOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_CLICKED,
            )
        )
//        binanceApi = SpotClientImpl(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY)
        binanceFuturesApi = UMFuturesClientImpl(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY)
        createScreenCaptureNotificationChannel()
        initOCREngine()
        val (width, height)  = ScreenUtils.getScreenResolution(this)
        multipleX = (width.toDouble() / 1080.toDouble()).let {
            "%.2f".format(it).toDouble()
        }
        multipleY = (height.toDouble() / 1920.toDouble()).let {
            "%.2f".format(it).toDouble()
        }
        initGate()
    }

    private fun createScreenCaptureNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val screenCaptureChannel = NotificationChannel(
                SCREEN_CAPTURE_CHANNEL_ID,
                SCREEN_CAPTURE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(screenCaptureChannel)
        }
    }

    private fun initGate(){
        GlobalScope.launch {
            withContext(Dispatchers.IO){
                val defaultClient: ApiClient = Configuration.getDefaultApiClient()
                defaultClient.setBasePath("https://api.gateio.ws/api/v4")
                defaultClient.setApiKeySecret("dc3a65828c1638222cf79f8cda058b05", "859ccc4a8fc9e46d9342d646a1da32ce9512f7e91204fb50aa6e790bf8f84e6f")
                accountApi = AccountApi(defaultClient)
                futuresApi = FuturesApi(defaultClient)
                walletApi = WalletApi(defaultClient)
            }
        }
    }

    private fun initOCREngine() {
        ocrEngine = OcrEngine(this.applicationContext)
    }

}