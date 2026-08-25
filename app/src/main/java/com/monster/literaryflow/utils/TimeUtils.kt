package com.monster.literaryflow.utils

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    //  判断传入的时间是否是当前时间，精确到分钟
    fun isCurrentTime(hour: Int, minute: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val inputTime = LocalTime.of(hour, minute)
            val currentTime = LocalTime.now()
            inputTime == currentTime
        } else {
            val dateFormat = SimpleDateFormat("HH:mm")
            val inputTimeStr = String.format("%02d:%02d", hour, minute)
            val currentTimeStr = dateFormat.format(Calendar.getInstance().time)
            inputTimeStr == currentTimeStr
        }
    }

    fun isInCurrentTime(time: Pair<Pair<Int, Int>, Pair<Int, Int>>): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 获取开始时间和结束时间
            val startTime = LocalTime.of(time.first.first, time.first.second)
            val endTime = LocalTime.of(time.second.first, time.second.second)
            val currentTime = LocalTime.now()
            // 判断当前时间是否在给定的开始和结束时间之间
            currentTime >= startTime && currentTime <= endTime
        } else {
            // Android版本小于O时使用旧的时间处理方式
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            // 获取开始时间和结束时间的小时与分钟
            val startHour = time.first.first
            val startMinute = time.first.second
            val endHour = time.second.first
            val endMinute = time.second.second

            // 将当前时间与开始时间和结束时间进行比较
            val currentTotalMinutes = currentHour * 60 + currentMinute
            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute

            // 判断当前时间是否在开始和结束时间之间
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        }
    }


    fun isTimeA(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek
            dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.FRIDAY
        } else {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.FRIDAY
        }

    }

    fun isTimeB(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek
            return dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.THURSDAY || dayOfWeek == DayOfWeek.SUNDAY
        } else {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            return dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY || dayOfWeek == Calendar.SUNDAY
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getNowTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date(System.currentTimeMillis())
        return sdf.format(date)
    }

    fun getTimeDiff(input: String) {
        val time = input.substringAfter("时间")


        System.currentTimeMillis()
        // 解析输入的UTC时间
        val utcTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ZonedDateTime.parse(time, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        // 转换为UTC+8时间
        val utcPlus8Time = utcTime.withZoneSameInstant(ZoneId.of("UTC+8"))
        // 打印转换后的时间
        Log.d("getTimeDiff", "UTC+8 Time: $utcPlus8Time")
        // 获取当前时间的UTC+8时间
        val currentTime = LocalDateTime.now(ZoneId.of("UTC+8"))
        val currentTimeZoned = currentTime.atZone(ZoneId.of("UTC+8"))
        // 计算两个时间的差值（以秒为单位）
        val secondsDifference = ChronoUnit.SECONDS.between(utcPlus8Time, currentTimeZoned)
        // 打印时间差
        Log.d("getTimeDiff", "相差: $secondsDifference S")

    }

    fun formatTime(time: Pair<Int, Int>): String {
        val (hour, minute) = time
        val hourString = hour.toString().padStart(2, '0')
        val minuteString = minute.toString().padStart(2, '0')
        return "$hourString:$minuteString"
    }


    fun isToday(lastTimeMillis: Long): Boolean {
        if (lastTimeMillis ==0L) return false
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val currentDate = dateFormat.format(Date(currentTime))
        val lastDate = dateFormat.format(Date(lastTimeMillis))

        return currentDate == lastDate
    }


}