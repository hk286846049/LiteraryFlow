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
    private val imageReader by lazy { ImageReader.newInstance(ScreenUtils.getHasVirtualKey(this), ScreenUtils.getScreenWidth(), PixelFormat.RGBA_8888, 1) }


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

object ImageUtils {

    //imageReader.acquireLatestImage()获取到的Image
    fun imageToBitmap(image: Image): Bitmap {
        Log.d("~~~", "imageToBitmap, width: = ${image.width}, height: = ${image.height}")
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        Log.d("~~~", "imageToBitmap, planes: = ${ planes}, buffer: = ${buffer}")
        Log.d("~~~", "imageToBitmap, pixelStride: = ${ planes[0].pixelStride}, rowStride: = ${ planes[0].rowStride}")
        val rowPadding = rowStride - pixelStride * image.width
        var bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val byteArray = ByteArray(rowStride * image.height)
        buffer[byteArray]
        var offset = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                var pixel = 0
                pixel = pixel or (byteArray[offset].toInt() and 0xff shl 16) // R
                pixel = pixel or (byteArray[offset + 1].toInt() and 0xff shl 8) // G
                pixel = pixel or (byteArray[offset + 2].toInt() and 0xff) // B
                pixel = pixel or (byteArray[offset + 3].toInt() and 0xff shl 24) // A
                bitmap.setPixel(x, y, pixel)
                offset += pixelStride
            }
            offset += rowPadding
        }
        image.close()
        return bitmap


/*
        // Rotate the bitmap if necessary
        if (ScreenUtils.isLandscape(MyApp.instance)) {
            val matrix = Matrix()
            matrix.postRotate(180f)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
*/

 /*       val width = image.width
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
        image.close()*/
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

