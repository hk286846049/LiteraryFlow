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
    fun getNowTime():String{
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date(System.currentTimeMillis())
        return sdf.format(date)
    }

    fun getTimeDiff(input:String){
        val time =  input.substringAfter("时间")


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
        Log.d("getTimeDiff","UTC+8 Time: $utcPlus8Time")
        // 获取当前时间的UTC+8时间
        val currentTime = LocalDateTime.now(ZoneId.of("UTC+8"))
        val currentTimeZoned = currentTime.atZone(ZoneId.of("UTC+8"))
        // 计算两个时间的差值（以秒为单位）
        val secondsDifference = ChronoUnit.SECONDS.between(utcPlus8Time, currentTimeZoned)
        // 打印时间差
        Log.d("getTimeDiff","相差: $secondsDifference S")

    }

}