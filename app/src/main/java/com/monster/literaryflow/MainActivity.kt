package com.monster.literaryflow

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import cn.coderpig.cp_fast_accessibility.getDrawableRes
import cn.coderpig.cp_fast_accessibility.getStringRes
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.logD
import cn.coderpig.cp_fast_accessibility.requireAccessibility
import cn.coderpig.cp_fast_accessibility.shortToast
import com.monster.literaryflow.Const.Companion.CqPackage
import com.monster.literaryflow.Const.Companion.JrPackage
import com.monster.literaryflow.Const.Companion.KgPackage
import com.monster.literaryflow.databinding.ActivityMainBinding
import com.monster.literaryflow.floatwindow.MainActivity2
import com.monster.literaryflow.photoScreen.ScreenActivity
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.service.MyNanoHttpService
import com.monster.literaryflow.service.MyNanoHttpdServer
import com.monster.literaryflow.service.WebsiteCheckerService
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.ScreenUtils
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration


@SuppressLint("WrongConstant")
class MainActivity : AppCompatActivity() {

    companion object{
        const val REQUEST_MEDIA_PROJECTION = 1
        const val REQUEST_CODE_OVERLAY_PERMISSION = 2
    }
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
    }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var floatingView:View? = null
    private val mClickListener = View.OnClickListener {
        when (it.id) {
            R.id.iv_service_status -> {
                if (isAccessibilityEnable) shortToast(getStringRes(R.string.service_is_enable_tips))
                else requireAccessibility()
            }

            R.id.tv_screen -> {
                startActivity(Intent(this@MainActivity, ScreenActivity::class.java))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }


    private fun initView() {
        binding.ivServiceStatus.setOnClickListener(mClickListener)
        binding.tvScreen.setOnClickListener(mClickListener)
        binding.tvOcr.setOnClickListener {
            startActivity(Intent(this@MainActivity, GalleryActivity::class.java))
        }
        binding.tvTest.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            }else{
                showFloatWindow()
            }
        }
        binding.btAppA.setOnClickListener {
            MyApp.runRuleApp = JrPackage
            startScreen()
        }
        binding.btAppB.setOnClickListener {
            MyApp.runRuleApp = KgPackage
            startScreen()
        }

        binding.btAppE.setOnClickListener {
            MyApp.runRuleApp = CqPackage
            startScreen()
        }
        binding.btCloseMj.isChecked = MyApp.isMj
        binding.btCloseMj.setOnClickListener {
            MyApp.isMj = !MyApp.isMj
        }
        binding.btCloseJd.isChecked = MyApp.isJd
        binding.btCloseJd.setOnClickListener {
            MyApp.isJd = !MyApp.isJd
        }
        binding.btCloseMh.isChecked = MyApp.isMh
        binding.btCloseMh.setOnClickListener {
            MyApp.isMh = !MyApp.isMh
        }
        binding.btCloseKf.isChecked = MyApp.isKf
        binding.btCloseKf.setOnClickListener {
            MyApp.isKf = !MyApp.isKf
        }
        binding.tvOpenHttp.setOnClickListener {
            //实例化 获取ip 地址
            val ip = getIPAddress()
            ip?.let { it1 -> logD(it1) }
            MyNanoHttpdServer.getInstance(ip)
            //启动服务监听
            startService( Intent(this, MyNanoHttpService::class.java))
        }
        val serviceIntent = Intent(this, WebsiteCheckerService::class.java)
        startService(serviceIntent)
        val str = "opsa"
        Log.d("logs","contains"+str.contains("op"))

    }

    fun startScreen() {
        imageReader = AppUtils.initImageReader(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, CaptureService::class.java))
        }else{
            startService(Intent(this, CaptureService::class.java))
        }
        startScreenCapture()
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnable) {
            binding.ivServiceStatus.setImageDrawable(getDrawableRes(R.drawable.ic_service_enable))
            binding.tvServiceStatus.text = getStringRes(R.string.service_status_enable)
//            FastAccessibilityService.showForegroundNotification(
//                "守护最好的坤坤🐤", "用来保护的，不用理我", "提示信息",
//                activityClass = MainActivity::class.java
//            )
        } else {
            binding.ivServiceStatus.setImageDrawable(getDrawableRes(R.drawable.ic_service_disable))
            binding.tvServiceStatus.text = getStringRes(R.string.service_status_disable)
        };
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode){
            REQUEST_MEDIA_PROJECTION ->{
                if (resultCode != RESULT_OK) {
                    return
                }
                Log.d("~~~", "Starting screen capture")
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
                virtualDisplay = if (MyApp.runRuleApp == JrPackage) {
                    mediaProjection!!.createVirtualDisplay(
                        "ScreenCapture",
                        ScreenUtils.getScreenWidth(),
                        ScreenUtils.getHasVirtualKey(this),
                        ScreenUtils.getScreenDensityDpi(),
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader?.surface,
                        null,
                        null
                    )
                } else {
                    mediaProjection!!.createVirtualDisplay(
                        "ScreenCapture",
                        ScreenUtils.getHasVirtualKey(this),
                        ScreenUtils.getScreenWidth(),
                        ScreenUtils.getScreenDensityDpi(),
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader?.surface,
                        null,
                        null
                    )
                }
                MyApp.virtualDisplay = virtualDisplay!!
                MyApp.imageReader = imageReader
                MyApp.mediaProjection = mediaProjection!!
                MyApp.runRuleApp?.let { AppUtils.openApp(this@MainActivity, it) }
            }
            REQUEST_CODE_OVERLAY_PERMISSION ->{
                showFloatWindow()
            }
        }

    }

    private fun startScreenCapture() {
        if (mediaProjection == null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            )
        }
    }

    private fun stopScreenCapture() {
        Log.d("~~~", "stopScreenCapture, virtualDisplay = $virtualDisplay")
        virtualDisplay?.release()
        virtualDisplay = null
    }

    /**
     * 获得IP地址，分为两种情况:
     * 一：是wifi下；
     * 二：是移动网络下；
     */
    private  fun getIPAddress(): String? {
        val context: Context = this@MainActivity
        val info: NetworkInfo = (context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo!!
        if (info.isConnected) {
            if (info.type === ConnectivityManager.TYPE_MOBILE) { //当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val intf: NetworkInterface = en.nextElement()
                        val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                        while (enumIpAddr.hasMoreElements()) {
                            val inetAddress: InetAddress = enumIpAddr.nextElement()
                            if (!inetAddress.isLoopbackAddress() && inetAddress is Inet4Address) {
                                return inetAddress.getHostAddress()
                            }
                        }
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
            } else if (info.type === ConnectivityManager.TYPE_WIFI) { //当前使用无线网络
                val wifiManager: WifiManager =
                    context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiManager.connectionInfo
                //调用方法将int转换为地址字符串
                return intIP2StringIP(wifiInfo.ipAddress)
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null
    }

    /**
     * 将得到的int类型的IP转换为String类型
     * @param ip
     * @return
     */
    private fun intIP2StringIP(ip: Int): String {
        return (ip and 0xFF).toString() + "." +
                (ip shr 8 and 0xFF) + "." +
                (ip shr 16 and 0xFF) + "." +
                (ip shr 24 and 0xFF)
    }

    private fun showFloatWindow(){
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY // Android 8.0+
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START // 初始位置
        layoutParams.x = 0
        layoutParams.y = 0
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        windowManager.addView(floatingView, layoutParams)
        floatingView!!.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
    }
}

