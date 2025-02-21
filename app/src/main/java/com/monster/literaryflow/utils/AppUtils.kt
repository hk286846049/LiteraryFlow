package com.monster.literaryflow.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.media.ImageReader
import android.os.Build
import android.util.Log
import android.widget.Toast
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
            // 获取应用的标签名和包名
            val label = context.packageManager.getApplicationLabel(it).toString()
            val packageName = it.packageName

            // 判断是否是游戏类型
            val isGame = isGameApp(it)

            // 根据是否是游戏类型将其分类
            appList.add(
                AppData(
                    label,
                    packageName,
                    isGame
                )
            )
        }
        return appList
    }

    // 判断是否为游戏类型
    private fun isGameApp(appInfo: ApplicationInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appInfo.category == ApplicationInfo.CATEGORY_GAME
        } else {
           false
        }
    }


    @SuppressLint("WrongConstant")
    fun initImageReader(context: Activity, isHor: Boolean = false): ImageReader {
        if (isHor) {
            return ImageReader.newInstance(
                ScreenUtils.getHasVirtualKey(context),
                ScreenUtils.getScreenWidth(),
                PixelFormat.RGBA_8888,
                1
            )
        } else {
            return ImageReader.newInstance(
                ScreenUtils.getScreenWidth(),
                ScreenUtils.getHasVirtualKey(context),
                PixelFormat.RGBA_8888,
                1
            )

        }


    }

    fun openApp(context: Context, packageName: String) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            Log.d("#####MONSTER#####", "open App:$packageName")
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "应用未安装", Toast.LENGTH_SHORT).show();
        }
    }

    fun isAppInForeground(context: Context, packageName: String): Boolean {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        // 获取最近一分钟内的应用使用状态
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 10 * 1000,
            currentTime
        )

        if (usageStats.isNullOrEmpty()) {
            Log.d("#####MONSTER#####", "recentStats.packageName :usageStats.isNullOrEmpty}")

            return false
        }

        // 找出最后使用的应用
        val recentStats = usageStats.maxByOrNull { it.lastTimeUsed }
        Log.d("#####MONSTER#####", "recentStats.packageName ${recentStats?.packageName}")

        return recentStats?.packageName == packageName
    }


    @SuppressLint("ServiceCast")
    fun closeApp(context: Context, packageName: String) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses(packageName)
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        val packageManager = context.packageManager

        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

}