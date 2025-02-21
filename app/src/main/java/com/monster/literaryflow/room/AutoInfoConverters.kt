package com.monster.literaryflow.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.TriggerBean

class AutoInfoConverters {

    private val gson = Gson()

    // Convert `Pair<Pair<Int, Int>, Pair<Int, Int>>` to String
    @TypeConverter
    fun fromRunTime(runTime: Pair<Pair<Int, Int>, Pair<Int, Int>>?): String? {
        return gson.toJson(runTime)
    }

    @TypeConverter
    fun fromTodayRunTime(todayRunTime: Pair<Long, Int>?): String? {
        return gson.toJson(todayRunTime)
    }

    // Convert String to `Pair<Pair<Int, Int>, Pair<Int, Int>>`
    @TypeConverter
    fun toRunTime(runTimeString: String?): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val type = object : TypeToken<Pair<Pair<Int, Int>, Pair<Int, Int>>>() {}.type
        return gson.fromJson(runTimeString, type)
    }

    @TypeConverter
    fun toTodayRunTime(todayRunTime: String?): Pair<Long, Int>? {
        val type = object : TypeToken<Pair<Long, Int>>() {}.type
        return gson.fromJson(todayRunTime, type)
    }

    // Convert `MutableList<RunBean>` to String
    @TypeConverter
    fun fromRunInfo(runInfo: MutableList<RunBean>?): String? {
        return gson.toJson(runInfo)
    }

    // Convert String to `MutableList<RunBean>`
    @TypeConverter
    fun toRunInfo(runInfoString: String?): MutableList<RunBean>? {
        val type = object : TypeToken<MutableList<RunBean>>() {}.type
        return gson.fromJson(runInfoString, type)
    }

    // Convert `MutableList<TriggerBean>` to String
    @TypeConverter
    fun fromMonitorList(monitorList: MutableList<TriggerBean>?): String? {
        return gson.toJson(monitorList)
    }

    // Convert String to `MutableList<TriggerBean>`
    @TypeConverter
    fun toMonitorList(monitorListString: String?): MutableList<TriggerBean>? {
        val type = object : TypeToken<MutableList<TriggerBean>>() {}.type
        return gson.fromJson(monitorListString, type)
    }
}

