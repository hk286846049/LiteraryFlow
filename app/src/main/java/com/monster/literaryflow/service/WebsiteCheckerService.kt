package com.monster.literaryflow.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.monster.literaryflow.utils.OrderUtils
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class WebsiteCheckerService : Service() {

    private var serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = "https://www.google.com"
        val maxRetries = 10

        serviceScope.launch {
            while (true) {
//                if (OrderUtils.getOpenOrders("PEPEUSDT").isNotEmpty()){
//                    Log.e("当前有订单","")
//                }
                delay(3 * 60 * 1000) // 暂停3分钟
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun checkWebsite(url: String, maxRetries: Int):Boolean {
        var retryCount = 0
        var isConnect = false
        while (retryCount < maxRetries) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("Website $url is accessible")
                    isConnect = true
                    break
                } else {
                    println("Error accessing website $url: Response code $responseCode")
                }
            } catch (e: Exception) {
                println("Error accessing website $url: ${e.message}")
            }
            retryCount++

            if (retryCount < maxRetries) {
                println("Retrying in 5 seconds...")
                delay(5000) // 暂停5秒
            }
        }
        return isConnect
    }
}
