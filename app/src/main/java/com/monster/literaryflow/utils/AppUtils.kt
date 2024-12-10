package com.monster.literaryflow.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.media.ImageReader
import android.util.Log
import android.widget.Toast
import com.monster.literaryflow.Const.Companion.JrPackage
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.bean.AppData

object AppUtils {
    fun getAllInstalledApps(context: Context): List<AppData> {
        val packageManager = context.packageManager
        // 获取所有已安装的应用程序信息
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        // 过滤掉系统应用
        return getAppData(context, allApps.filterNot { isSystemApp(it) })
    }

    private fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    private fun getAppData(context: Context, list: List<ApplicationInfo>): List<AppData> {
        val appList = mutableListOf<AppData>()
        list.forEach {
            appList.add(
                AppData(
                    context.packageManager.getApplicationLabel(it).toString(),
                    getAppIcon(context, it.packageName)
                )
            )
        }
        return appList
    }

    @SuppressLint("WrongConstant")
    fun initImageReader(context: Activity):ImageReader{
        when(MyApp.runRuleApp){
            JrPackage ->{
                return ImageReader.newInstance( ScreenUtils.getScreenWidth(),ScreenUtils.getHasVirtualKey(context), PixelFormat.RGBA_8888, 1)
            }
            else ->{
                return ImageReader.newInstance(ScreenUtils.getHasVirtualKey(context), ScreenUtils.getScreenWidth(), PixelFormat.RGBA_8888, 1)

            }
        }

    }
     fun openApp(context: Context, packageName:String) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "应用未安装", Toast.LENGTH_SHORT).show();
        }
    }
    @SuppressLint("ServiceCast")
    fun closeApp(context: Context, packageName: String) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses(packageName)
    }

    private fun getAppIcon(context: Context, packageName: String): Drawable? {
        val packageManager = context.packageManager

        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

}