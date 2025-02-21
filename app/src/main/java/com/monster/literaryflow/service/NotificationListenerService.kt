package com.monster.literaryflow.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import kotlinx.coroutines.*

class NotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListenerService", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotificationListenerService", "onListenerDisconnected")
        requestRebind(ComponentName(this, NotificationListenerService::class.java))
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        Log.d("NotificationListenerService", "onNotificationPosted")
        val notification = sbn?.notification ?: return
        val extras = notification.extras
        val title = extras?.getString(Notification.EXTRA_TITLE, "") ?: ""

        if (!TextUtils.isEmpty(title) && title.contains("警报")) {
            handleNotification(extras.getString(Notification.EXTRA_TEXT, ""))
        }
        cancelAllNotifications()
    }




    private fun handleNotification(content: String) {

    }

}
