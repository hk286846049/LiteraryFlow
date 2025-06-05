package com.monster.literaryflow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.requireAccessibility
import cn.coderpig.cp_fast_accessibility.shortToast
import com.monster.literaryflow.autoRun.view.AutoListActivity
import com.monster.literaryflow.databinding.ActivityMainBinding
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.PermissionUtils

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
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
                    android.Manifest.permission.CAPTURE_AUDIO_OUTPUT
                ),
                REQUEST_CAPTURE_AUDIO_OUTPUT
            )
        }
        
        initView()
        observeSharedData()
    }

    private fun observeSharedData() {
        SharedData.trigger.observe(this) { value ->
            Log.d("MainActivity", "trigger: $value")
            when (value) {
                "打开录屏|竖屏" -> {
                    findHorText = false
                    MyApp.imageReader = AppUtils.initImageReader(this)
                    startScreenCapture()
                }
                "打开录屏|横屏" -> {
                    findHorText = true
                    MyApp.imageReader = AppUtils.initImageReader(this, true)
                    startScreenCapture()
                }
            }
            SharedData.trigger.postValue("")
        }
    }

    private fun initView() {
        // Media Center buttons
        binding.btnVideo.setOnClickListener {
            shortToast("视频功能即将推出！")
        }

        binding.btnMusic.setOnClickListener {
            shortToast("音乐功能即将推出！")
        }

        // Control buttons
        binding.btnPermissions.setOnClickListener {
            if (!PermissionUtils.hasUsageStatsPermission(this)) {
                PermissionUtils.requestUsageStatsPermission(this)
            }
        }

        binding.btnAuto.setOnClickListener {
            startActivity(Intent(this, AutoListActivity::class.java))
        }

        binding.btnScreenCapture.setOnClickListener {
            findHorText = false
            MyApp.imageReader = AppUtils.initImageReader(this)
            startScreenCapture()
        }

        binding.btnStopCapture.setOnClickListener {
            stopScreenCapture()
        }

        // Settings
        binding.switchFloating.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            } else if (isChecked) {
                startFloatingWindowService()
            }
        }
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
        if (!isAccessibilityEnable) {
            requireAccessibility()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK) {
                    startScreen(data)
                    Log.d("MainActivity", "Starting screen capture")
                }
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingWindowService()
                } else {
                    binding.switchFloating.isChecked = false
                }
            }
        }
    }

    private fun startScreenCapture() {
        if (MyApp.mediaProjection == null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            )
        }
    }

    private fun startFloatingWindowService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, FloatingWindowService::class.java))
        } else {
            startService(Intent(this, FloatingWindowService::class.java))
        }
    }

    private fun stopScreenCapture() {
        MyApp.mediaProjection?.stop()
        MyApp.virtualDisplay?.release()
        MyApp.virtualDisplay = null
        MyApp.mediaProjection = null
    }
}

