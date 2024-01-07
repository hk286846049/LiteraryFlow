package com.monster.literaryflow.photoScreen

import android.R.attr.path
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.coderpig.cp_fast_accessibility.sleep
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.databinding.ActivityScreenBinding
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.utils.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


const val REQUEST_MEDIA_PROJECTION = 1

@SuppressLint("WrongConstant")
class ScreenActivity : AppCompatActivity() {
    private val binding by lazy { ActivityScreenBinding.inflate(layoutInflater) }
    private val mediaProjectionManager: MediaProjectionManager by lazy { getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val imageReader by lazy { ImageReader.newInstance( ScreenUtils.getScreenWidth(),ScreenUtils.getHasVirtualKey(this), PixelFormat.RGBA_8888, 2) }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnStart.setOnClickListener {
            startForegroundService(Intent(this, CaptureService::class.java))
            startScreenCapture()
        }
        binding.btnStop.setOnClickListener {
            Log.d("~~~", "Stop screen capture")
            stopScreenCapture()
        }
/*
        lifecycleScope.launch(Dispatchers.IO) {
            while (true){
                if (MyApp.image!=null){
                    withContext(Dispatchers.Main) {
                        binding.iv.setImageBitmap(MyApp.image!!)
                    }
                }
                sleep(1000)
            }

        }
*/
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != RESULT_OK) {
                Log.d("~~~", "User cancelled")
                return
            }
            Log.d("~~~", "Starting screen capture")
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "ScreenCapture", ScreenUtils.getScreenWidth(),
                ScreenUtils.getHasVirtualKey(this),  ScreenUtils.getScreenDensityDpi(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )
            MyApp.virtualDisplay = virtualDisplay!!
            MyApp.imageReader = imageReader
            MyApp.mediaProjection = mediaProjection!!
        }
    }

    private fun startScreenCapture() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        } else {
        }
    }

    private fun stopScreenCapture() {
        Log.d("~~~", "stopScreenCapture, virtualDisplay = $virtualDisplay")
        virtualDisplay?.release()
        virtualDisplay = null
    }
}

