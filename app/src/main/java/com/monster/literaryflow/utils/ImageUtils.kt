package com.monster.literaryflow.utils

import android.R
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.monster.literaryflow.MyApp
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {
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
                Uri.parse("file://" + R.attr.path)
            )
        )
    }
}