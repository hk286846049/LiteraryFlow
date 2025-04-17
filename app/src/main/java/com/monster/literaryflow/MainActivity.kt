package com.monster.literaryflow

import android.annotation.SuppressLint
import android.app.Activity
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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import cn.coderpig.cp_fast_accessibility.getDrawableRes
import cn.coderpig.cp_fast_accessibility.getStringRes
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.logD
import cn.coderpig.cp_fast_accessibility.requireAccessibility
import cn.coderpig.cp_fast_accessibility.shortToast
import com.monster.literaryflow.autoRun.view.AddAutoActivity
import com.monster.literaryflow.autoRun.view.AutoListActivity
import com.monster.literaryflow.databinding.ActivityMainBinding
import com.monster.literaryflow.photoScreen.ScreenActivity
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.service.MyNanoHttpService
import com.monster.literaryflow.service.MyNanoHttpdServer
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.service.WebsiteCheckerService
import com.monster.literaryflow.ui.TestActivity
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.PermissionUtils
import com.monster.literaryflow.utils.ScreenUtils
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration


@SuppressLint("WrongConstant")
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1
        const val REQUEST_CODE_OVERLAY_PERMISSION = 2
        const val REQUEST_CAPTURE_AUDIO_OUTPUT = 3
    }

    private var findHorText = false

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
    }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var floatingView: View? = null
    private val mClickListener = View.OnClickListener {
        when (it.id) {
            R.id.iv_service_status -> {
                if (isAccessibilityEnable) shortToast(getStringRes(R.string.service_is_enable_tips))
                else requireAccessibility()
            }

            R.id.tv_screen -> {
                startActivity(Intent(this@MainActivity, TestActivity::class.java))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
                    android.Manifest.permission.CAPTURE_AUDIO_OUTPUT
                ),
                REQUEST_CAPTURE_AUDIO_OUTPUT
            )
        }
        initView()
        SharedData.trigger.observe(this) { value ->
            Log.d("MainActivity", "trigger: $value")
            if (value == "打开录屏|竖屏") {
                findHorText = false
                MyApp.imageReader = AppUtils.initImageReader(this)
                startScreenCapture()
            }else  if (value == "打开录屏|横屏"){
                findHorText = true
                MyApp.imageReader = AppUtils.initImageReader(this, true)
                startScreenCapture()
            }
        }

    }


    private fun initView() {


        binding.ivServiceStatus.setOnClickListener(mClickListener)
        binding.tvScreen.setOnClickListener(mClickListener)
        binding.tvOcr.setOnClickListener {
            val intent = Intent(this@MainActivity,AddAutoActivity::class.java)
            intent.putExtra("isVirtual",true)
            startActivity(intent)
        }
        binding.tvTest.setOnClickListener {

            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(Intent(this, FloatingWindowService::class.java))
                } else {
                    startService(Intent(this, FloatingWindowService::class.java))
                }
            }
        }
        binding.btAppA.setOnClickListener {
            if (!PermissionUtils.hasUsageStatsPermission(this)) {
                PermissionUtils.requestUsageStatsPermission(this)
            }
        }
        binding.btAppB.setOnClickListener {
            startActivity(Intent(this@MainActivity, AutoListActivity::class.java))
        }
        binding.btVerFind.setOnClickListener {
            findHorText = false
            imageReader = AppUtils.initImageReader(this)
            startScreenCapture()
        }
        binding.btClearTimes.setOnClickListener {
            stopScreenCapture()
        }

        binding.btHorFind.setOnClickListener {
            findHorText = true
            imageReader = AppUtils.initImageReader(this, true)
            startScreenCapture()
        }

        binding.btCloseMj.setOnClickListener {
        }
        binding.btCloseJd.setOnClickListener {
        }
        binding.btCloseMh.setOnClickListener {
        }
        binding.btCloseKf.setOnClickListener {
        }
        binding.tvOpenHttp.setOnClickListener {
            //实例化 获取ip 地址
            val ip = getIPAddress()
            ip?.let { it1 -> logD(it1) }
            MyNanoHttpdServer.getInstance(ip)
            //启动服务监听
            startService(Intent(this, MyNanoHttpService::class.java))
        }
        val serviceIntent = Intent(this, WebsiteCheckerService::class.java)
        startService(serviceIntent)
        val str = "opsa"
        Log.d("logs", "contains" + str.contains("op"))
    }

    private fun startScreen(data: Intent?) {
        val serviceIntent = Intent(this, CaptureService::class.java).apply {
            putExtra("resultCode", Activity.RESULT_OK)
            putExtra("data", data)
            putExtra("findHorText", findHorText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.fireView.startShow()
        if (isAccessibilityEnable) {
            binding.ivServiceStatus.setImageDrawable(getDrawableRes(R.drawable.ic_service_enable))
            binding.tvServiceStatus.text = getStringRes(R.string.service_status_enable)
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            }

        } else {
            requireAccessibility()
        };
    }

    override fun onPause() {
        super.onPause()
        binding.fireView.stopShow()
    }
    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            // 在这里处理 MediaProjection 停止时的逻辑，比如释放虚拟显示器等资源
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode != RESULT_OK) {
                    return
                }
                startScreen(data)
                Log.d("#####MONSTER#####", "Starting screen capture")

            }

            REQUEST_CODE_OVERLAY_PERMISSION -> {
                startService(Intent(this, FloatingWindowService::class.java))
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

    // 主动结束屏幕捕获的函数
    private fun stopScreenCapture() {
        // 停止 MediaProjection 捕获
        mediaProjection?.stop()
        // 释放 VirtualDisplay（通常在回调中也会处理）
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection = null
    }

    /**
     * 获得IP地址，分为两种情况:
     * 一：是wifi下；
     * 二：是移动网络下；
     */
    private fun getIPAddress(): String? {
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

    private fun showFloatWindow(onClickAction: () -> OnClickListener) {
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

        val screenWidth = resources.displayMetrics.widthPixels

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

                    MotionEvent.ACTION_UP -> {
                        // 计算释放时的位置，决定吸附到左侧或右侧
                        val middle = screenWidth / 2
                        layoutParams.x = if (layoutParams.x < middle) {
                            0 // 吸附到左侧
                        } else {
                            screenWidth - floatingView!!.width // 吸附到右侧
                        }
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
        if (floatingView!=null){
            windowManager.removeView(floatingView)
        }
    }


}

