package com.monster.literaryflow.service

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class AutoTileService  : TileService() {
    override fun onClick() {
        super.onClick()
        qsTile.state = Tile.STATE_ACTIVE

        // 根据当前磁贴状态，决定开启或关闭悬浮窗
        if (isFloatingWindowShowing()) {
            Log.d("AutoTileService", "悬浮窗已显示，showView")
            FloatingWindowService.instance?.showView()
        } else {
            Log.d("AutoTileService", "悬浮窗未显示，尝试开启")
            // 如果未显示，先判断是否有悬浮窗权限
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindow()
            } else {
                // 无权限，跳转到悬浮窗权限设置界面
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                // 注意：需要 FLAG_ACTIVITY_NEW_TASK，否则无法启动 Activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAndCollapse(intent)
            }
        }
        qsTile.updateTile()
    }

    /**
     * 判断悬浮窗是否正在显示。这里简单示例，实际项目中需要根据你启动悬浮窗 Service 的状态来判断
     */
    private fun isFloatingWindowShowing(): Boolean {
        // 可以采用绑定 Service 的方式或静态标志位来判断
        return FloatingWindowService.isCreate
    }

    /**
     * 启动悬浮窗 Service
     */
    private fun startFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, FloatingWindowService::class.java))
        } else {
            startService(Intent(this, FloatingWindowService::class.java))
        }

    }

}
