package com.monster.literaryflow.service

import android.service.quicksettings.TileService
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.Tile
import android.util.Log

class FloatingOcrTileService : TileService() {
    override fun onClick() {
        super.onClick()
        // 根据当前磁贴状态，决定开启或关闭悬浮窗
        if (isFloatingWindowShowing()) {
            Log.d("FloatingOcrTileService", "悬浮窗已显示，尝试关闭")
            // 如果悬浮窗已显示，则关闭
            stopFloatingWindow()
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            Log.d("FloatingOcrTileService", "悬浮窗未显示，尝试开启")
            // 如果未显示，先判断是否有悬浮窗权限
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindow()
                qsTile.state = Tile.STATE_ACTIVE
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
        return OcrFloatingWindowService.isRunning
    }

    /**
     * 启动悬浮窗 Service
     */
    private fun startFloatingWindow() {
        val intent = Intent(this, OcrFloatingWindowService::class.java)
        // 对于 Android O 及以上，建议使用 startForegroundService
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * 关闭悬浮窗 Service
     */
    private fun stopFloatingWindow() {
        val intent = Intent(this, OcrFloatingWindowService::class.java)
        stopService(intent)
    }
}
