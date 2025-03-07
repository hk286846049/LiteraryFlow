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
import androidx.lifecycle.MutableLiveData
import com.benjaminwan.ocrlibrary.OcrEngine
import com.monster.fastAccessibility.Const.Companion.multipleX
import com.monster.fastAccessibility.Const.Companion.multipleY
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.room.AppDatabase
import com.monster.literaryflow.utils.ScreenUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
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
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        val image: MutableLiveData<Bitmap> = MutableLiveData()
        var mediaProjection: MediaProjection? = null
        var orientation: Int = 0
        var widthPixels: Int = 0
        var heightPixels: Int = 0
        //当前运行app
        var runRuleApp:String ?=null
        var isHorRun = MutableLiveData(false)
        var isVerRun = MutableLiveData(false)
        var isLoopRun = MutableLiveData(false)
        var isUpdateData = MutableLiveData(false)

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FastAccessibilityService.init(
            instance, MyAccessibilityService::class.java, arrayListOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.WINDOWS_CHANGE_BOUNDS,
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            )
        )
        createScreenCaptureNotificationChannel()
        initOCREngine()
        val (width, height)  = ScreenUtils.getScreenResolution(this)
        multipleX = (width.toDouble() / 1080.toDouble()).let {
            "%.2f".format(it).toDouble()
        }
        multipleY = (height.toDouble() / 1920.toDouble()).let {
            "%.2f".format(it).toDouble()
        }
        reSetRunTimes()
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


    private fun initOCREngine() {
        ocrEngine = OcrEngine(this.applicationContext)
    }

    private fun reSetRunTimes(){
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(this@MyApp).autoInfoDao()
            val list = dao.getAll()
            list.forEachIndexed { index, autoInfo ->
                if (!TimeUtils.isToday(autoInfo.todayRunTime.first)) {
                    autoInfo.todayRunTime = Pair(0L, 0)
                    dao.update(autoInfo)
                }
            }
        }
    }
}