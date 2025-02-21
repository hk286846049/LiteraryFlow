package com.monster.literaryflow.bean


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.monster.literaryflow.room.AutoInfoConverters
import java.io.Serializable

@Entity(tableName = "auto_info")
@TypeConverters(AutoInfoConverters::class)
data class AutoInfo(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    var title: String? = null,
    var runState: Boolean = false,
    @ColumnInfo(name = "run_app_name")
    var runAppName: String? = null,
    @ColumnInfo(name = "run_package_name")
    var runPackageName: String? = null,
    @ColumnInfo(name = "run_times")
    var runTimes: Int = 1,
    @ColumnInfo(name = "run_time")
    var runTime: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
    @ColumnInfo(name = "app_level")
    var appLevel: Int = 0,
    @ColumnInfo(name = "run_info")
    var runInfo: MutableList<RunBean>? = null,
    @ColumnInfo(name = "is_run")
    var isRun: Boolean = false,
    @ColumnInfo(name = "today_run_time")
    var todayRunTime: Pair<Long,Int> = Pair(0L,0),
    @ColumnInfo(name = "monitor_list")
    var monitorList:MutableList<TriggerBean>? = null

):Serializable {
    @Ignore
    constructor() : this(0)
    override fun toString(): String {
        return "AutoInfo(id=$id, title=$title, runState=$runState, runAppName=$runAppName, runPackageName=$runPackageName, " +
                "runTimes=$runTimes, runTime=$runTime, appLevel=$appLevel, runInfo=${runInfo?.joinToString(", ")}, " +
                "isRun=$isRun, todayRunTime=$todayRunTime)"
    }
}


