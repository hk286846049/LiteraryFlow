package com.monster.literaryflow.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.content.ContextCompat.getSystemService
import com.monster.literaryflow.MyApp


object ScreenUtils {

    fun getScreenWidth(): Int {
        val widthPixels =  Resources.getSystem().displayMetrics.widthPixels
        MyApp.widthPixels = widthPixels
        return widthPixels
    }
    @SuppressLint("ServiceCast")
    fun getScreenResolution(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        return Pair(widthPixels, heightPixels)
    }
    fun getScreenHeight(context: Context): Int {
        Log.d("#####MONSTER#####", "getScreenHeight_old:${Resources.getSystem().displayMetrics.heightPixels} ")
        val height = getScreenResolution(context).second
        Log.d("#####MONSTER#####", "getScreenHeight_new:$height ")
        return 2800

    }
     fun getHasVirtualKey(context:Activity): Int {
        var dpi = 0
        val display: Display = context.windowManager.defaultDisplay
        val dm = DisplayMetrics()
        val c: Class<*>
        try {
            c = Class.forName("android.view.Display")
            val method = c.getMethod("getRealMetrics", DisplayMetrics::class.java)
            method.invoke(display, dm)
            dpi = dm.heightPixels
        } catch (e: Exception) {
            e.printStackTrace()
        }
         MyApp.heightPixels = dpi
        return dpi
    }
    fun getScreenDensityDpi(): Int {
        return Resources.getSystem().displayMetrics.densityDpi
    }
    //当前是横屏？
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    //当前是竖屏？
    fun isPortrait(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

}
