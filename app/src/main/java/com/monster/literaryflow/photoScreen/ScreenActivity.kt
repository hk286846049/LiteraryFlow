package com.monster.literaryflow.photoScreen

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.HardwareBuffer.RGBA_8888
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.monster.literaryflow.R
import com.monster.literaryflow.SCREEN_CAPTURE_CHANNEL_ID
import com.monster.literaryflow.databinding.ActivityScreenBinding
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.utils.ScreenUtils
import java.nio.ByteBuffer

const val REQUEST_MEDIA_PROJECTION = 1

@SuppressLint("WrongConstant")
class ScreenActivity : AppCompatActivity() {
    private val binding by lazy { ActivityScreenBinding.inflate(layoutInflater) }
    private val mediaProjectionManager: MediaProjectionManager by lazy { getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val imageReader by lazy { ImageReader.newInstance(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(), PixelFormat.RGBA_8888, 1) }


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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != RESULT_OK) {
                Log.d("~~~", "User cancelled")
                return
            }
            Log.d("~~~", "Starting screen capture")
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            setUpVirtualDisplay()
        }
    }

    private fun startScreenCapture() {
        if (mediaProjection == null) {
            Log.d("~~~", "Requesting confirmation")
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        } else {
            Log.d("~~~", "mediaProjection != null")
            setUpVirtualDisplay()
        }
    }

    private fun setUpVirtualDisplay() {
        Log.d("~~~", "setUpVirtualDisplay")
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(), ScreenUtils.getScreenDensityDpi(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        handler.postDelayed({
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                Log.d("~~~", "get image: $image")
                binding.iv.setImageBitmap(ImageUtils.imageToBitmap(image))
            } else {
                Log.d("~~~", "image == null")
            }
            stopScreenCapture()
        }, 3000)
    }

    private fun stopScreenCapture() {
        Log.d("~~~", "stopScreenCapture, virtualDisplay = $virtualDisplay")
        virtualDisplay?.release()
        virtualDisplay = null
    }
}

object ImageUtils {
    fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        //两个像素的距离
        val pixelStride = planes[0].pixelStride
        //整行的距离
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        var bitmap =
            Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        return bitmap
    }
}