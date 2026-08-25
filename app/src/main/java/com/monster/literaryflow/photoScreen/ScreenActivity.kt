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
import com.monster.literaryflow.MainActivity.Companion.REQUEST_MEDIA_PROJECTION
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.databinding.ActivityScreenBinding
import com.monster.literaryflow.rule.ComposeMyAppActivity
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.utils.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException



@SuppressLint("WrongConstant")
class ScreenActivity : AppCompatActivity() {
    private val binding by lazy { ActivityScreenBinding.inflate(layoutInflater) }
    private val mediaProjectionManager: MediaProjectionManager by lazy { getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val imageReader by lazy { ImageReader.newInstance(ScreenUtils.getHasVirtualKey(this),ScreenUtils.getScreenWidth(),  PixelFormat.RGBA_8888, 1) }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btAddApp.setOnClickListener {
            startActivity(Intent(this@ScreenActivity, ComposeMyAppActivity::class.java))
        }
        binding.btnStart.setOnClickListener {
            startForegroundService(Intent(this, CaptureService::class.java))
            startScreenCapture()
        }
        binding.btnStop.setOnClickListener {
            Log.d("#####MONSTER#####", "Stop screen capture")
            stopScreenCapture()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            while (true){
                if (MyApp.image.value!=null){
                    withContext(Dispatchers.Main) {
                        binding.iv.setImageBitmap(MyApp.image.value)
                    }
                }
                sleep(1000)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_MEDIA_PROJECTION ->{
                if (resultCode != RESULT_OK) {
                    Log.d("#####MONSTER#####", "User cancelled")
                    return
                }
                Log.d("#####MONSTER#####", "Starting screen capture")
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
                virtualDisplay = mediaProjection!!.createVirtualDisplay(
                    "ScreenCapture",
                    ScreenUtils.getHasVirtualKey(this), ScreenUtils.getScreenWidth(), ScreenUtils.getScreenDensityDpi(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, null
                )
                MyApp.virtualDisplay = virtualDisplay!!
                MyApp.imageReader = imageReader
                MyApp.mediaProjection = mediaProjection!!
            }
        }

    }

    private fun startScreenCapture() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        }
    }

    private fun stopScreenCapture() {
        Log.d("#####MONSTER#####", "stopScreenCapture, virtualDisplay = $virtualDisplay")
        virtualDisplay?.release()
        virtualDisplay = null
    }
}

object ImageUtils {

    //imageReader.acquireLatestImage()获取到的Image
    fun imageToBitmap(image: Image): Bitmap {
        Log.d("#####MONSTER#####", "imageToBitmap, width: = ${image.width}, height: = ${image.height}")

        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride

        Log.d("#####MONSTER#####", "imageToBitmap, planes: = ${planes}, buffer: = ${buffer}")
        Log.d("#####MONSTER#####", "imageToBitmap, pixelStride: = $pixelStride, rowStride: = $rowStride")

        val rowPadding = rowStride - pixelStride * image.width

        // Create Bitmap
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)

        // Directly access the buffer and assign to the bitmap
        val pixelArray = IntArray(image.width * image.height)
        var offset = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                var pixel = 0
                pixel = pixel or (buffer[offset].toInt() and 0xff shl 16)  // R
                pixel = pixel or (buffer[offset + 1].toInt() and 0xff shl 8)  // G
                pixel = pixel or (buffer[offset + 2].toInt() and 0xff)  // B
                pixel = pixel or (buffer[offset + 3].toInt() and 0xff shl 24)  // A
                pixelArray[y * image.width + x] = pixel
                offset += pixelStride
            }
            offset += rowPadding
        }

        // Set pixels in bitmap
        bitmap.setPixels(pixelArray, 0, image.width, 0, 0, image.width, image.height)

        image.close()
        return bitmap
    }
    fun saveImageToGallery( bmp: Bitmap) {
        // 首先保存图片
        val appDir = File(Environment.getExternalStorageDirectory(), "LiteraryFlow")
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(
                MyApp.instance.contentResolver,
                file.absolutePath, fileName, null
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        // 最后通知图库更新
        MyApp.instance.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + path)
            )
        )
    }

}

