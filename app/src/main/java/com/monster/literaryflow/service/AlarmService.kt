package com.monster.literaryflow.service

// AlarmService.kt
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.monster.literaryflow.R

class AlarmService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private lateinit var stopHandler: Handler
    private lateinit var stopReceiver: BroadcastReceiver

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
        const val STOP_ACTION = "STOP_ALARM"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeMediaPlayer()
//        setupStopReceiver()
        stopHandler = Handler(Looper.getMainLooper())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm notifications"
            }
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // 设置你的闹钟铃声资源（放在res/raw目录）
            val descriptor = resources.openRawResourceFd(R.raw.alarm_sound)
            setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            descriptor.close()
            isLooping = true
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

/*
    private fun setupStopReceiver() {
        stopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopSelf()
            }
        }

        // 启动服务前检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
                return
            }
        }else{
            registerReceiver(stopReceiver, IntentFilter(STOP_ACTION))

        }
    }
*/


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        stopHandler.postDelayed({ stopSelf() }, 60000) // 60秒后自动停止
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = STOP_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto任务完成")
            .setContentText("点击停止闹钟")
            .setSmallIcon(R.drawable.alarm)
            .addAction(R.drawable.stop, "停止", pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        unregisterReceiver(stopReceiver)
        stopHandler.removeCallbacksAndMessages(null)
        notificationManager.cancel(NOTIFICATION_ID)
    }
}