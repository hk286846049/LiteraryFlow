package com.monster.literaryflow.bean

import android.graphics.drawable.Drawable
import java.io.Serializable
import java.net.URL

data class AppData(val appName:String, val packageName:String, var isGame:Boolean, var isCollect:Boolean = false, var isLandscape: Boolean = false):
    Serializable {
}