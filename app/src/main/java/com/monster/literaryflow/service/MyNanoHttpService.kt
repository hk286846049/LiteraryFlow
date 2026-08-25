package com.monster.literaryflow.service

import android.app.Service
import android.content.Intent
import android.os.IBinder


class MyNanoHttpService : Service() {
    private val httpServer = MyNanoHttpdServer.getInstance(null)
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            httpServer!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
            startService(Intent(this, MyNanoHttpService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        httpServer!!.stop()
    }
}

