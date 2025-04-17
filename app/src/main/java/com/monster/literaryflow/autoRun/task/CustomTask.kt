package com.monster.literaryflow.autoRun.task

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import cn.coderpig.cp_fast_accessibility.back
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.sleep
import cn.coderpig.cp_fast_accessibility.startSliding
import cn.coderpig.cp_fast_accessibility.stopSliding
import cn.coderpig.cp_fast_accessibility.swipe
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MainActivity
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.AutoRunType
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.room.AutoInfoDao
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import taylor.com.util.Preference


class CustomTask(val context: Context, val autoInfoDao: AutoInfoDao) {
    private val taskChannel = Channel<List<AutoInfo>>(Channel.UNLIMITED)
    private var taskJob: Job? = null
    private val _state = MutableStateFlow<TaskState>(TaskState.Idle)
    private var accessibilityService: MyAccessibilityService? = null
    private var scheduler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        TaskScheduler()
    } else {
        TODO("VERSION.SDK_INT < O")
    }
    var onStatusUpdate: ((taskName: String, loopType: AutoRunType?, progress: Int, total: Int) -> Unit)? =
        null

    sealed class TaskState {
        object Idle : TaskState()
        object Running : TaskState()
        object Paused : TaskState()
        object Cancelled : TaskState()
    }

    fun start() {
        if (_state.value == TaskState.Running) return
        Log.i("任务调度", "====== 启动任务调度器 ======")
        accessibilityService = FastAccessibilityService.instance as? MyAccessibilityService
        _state.value = TaskState.Running
        taskJob = CoroutineScope(Dispatchers.IO).launch {
            for (data in taskChannel) {
                if (_state.value == TaskState.Paused) {
                    delay(100)
                    continue
                }
                if (_state.value == TaskState.Cancelled) break
                Log.d("任务处理", "接收到新任务数据，共${data.size}条")
                process(data)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scheduler = TaskScheduler()
        }
    }

    fun cancel() {
        Log.i("任务调度", "====== 取消所有任务 ======")
        _state.value = TaskState.Cancelled
        taskJob?.cancel()
        taskJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scheduler.cancelAll()
        }
        stopSliding()
    }

    fun sendData(data: List<AutoInfo>) {
        if (_state.value != TaskState.Cancelled) {
            Log.d("任务分发", "分发新任务数据到通道")
            taskChannel.trySend(data)
        }
    }

    private suspend fun process(data: List<AutoInfo>?) {
        Log.d("任务处理", "开始处理任务数据")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            scheduler.removeAll()
            runAuto(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun runAuto(list: List<AutoInfo>?) {
        if (list == null) return
        Log.i("自动任务", "====== 开始执行自动任务 ======")
        list.forEach { autoInfo ->
            if (autoInfo.isRun) {
                if (MyApp.imageReader == null && autoInfo.runInfo?.find { it.clickBean?.isFindText4Node == false || it.triggerBean?.isFindText4Node == false } != null) {
                    val nowApp = MyAccessibilityService.tPackageName
                    val landscapePrefs = context.getSharedPreferences(
                        "landscape_apps",
                        AppCompatActivity.MODE_PRIVATE
                    )
                    val landscapePreferences = Preference(landscapePrefs)
                    val landscapeApps: Set<String> =
                        landscapePreferences["landscape_apps", emptySet()]
                    val isGame = landscapeApps.contains(nowApp)
                    AppUtils.openApp(MyApp.instance, "com.monster.literaryflow")
                    delay(2000)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("key", "value") // 传递数据（可选）
                    }
                    context.startActivity(intent)
                    delay(1000)
                    if (isGame) {
                        SharedData.trigger.postValue("打开录屏|横屏")
                    } else {
                        SharedData.trigger.postValue("打开录屏|竖屏")
                    }
                    delay(500)
                    //判断系统版本是否为Android 15
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (accessibilityService != null) {
                            accessibilityService!!.clickText("单个应用", TextPickType.EXACT_MATCH)
                            delay(1000)
                            accessibilityService!!.clickText("整个屏幕", TextPickType.EXACT_MATCH)
                            delay(1000)
                            accessibilityService!!.clickText("立即开始", TextPickType.EXACT_MATCH)
                            delay(1000)
                        } else {
                            Log.e("无障碍服务", "无障碍服务未激活")
                        }
                    }
                    nowApp?.let { AppUtils.openApp(MyApp.instance, it) }
                }else{
                    Log.d("自动任务", "MyApp.imageReader: ${MyApp.imageReader}")
                }
                Log.i("任务执行", "处理任务: ${autoInfo.title} | 类型: ${autoInfo.loopType}")
                when (autoInfo.loopType) {
                    AutoRunType.SPECIFIED_NUMBER -> {
                        if (TimeUtils.isToday(autoInfo.todayRunTime.first)) {
                            if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                                Log.d(
                                    "任务执行",
                                    "今日已运行${autoInfo.todayRunTime.second}次，还需运行${autoInfo.runTimes - autoInfo.todayRunTime.second}次"
                                )
                                runLoop(autoInfo.runTimes - autoInfo.todayRunTime.second, autoInfo)
                            } else {
                                Log.d("任务执行", "今日运行次数已达上限")
                            }
                        } else {
                            autoInfo.todayRunTime = Pair(0L, 0)
                            autoInfoDao.update(autoInfo)
                            Log.d("任务执行", "新的一天，重置运行次数")
                            runLoop(autoInfo.runTimes, autoInfo)
                        }
                    }

                    AutoRunType.LOOP -> {
                        Log.i("循环任务", "设置循环任务 | 间隔: ${autoInfo.sleepTime}秒")
                        scheduler.addTask(
                            ScheduledTask(
                                autoInfo.title!!,
                                TaskSchedule.Interval(autoInfo.sleepTime * 1000L)
                            ) {
                                runLoop(1, autoInfo)
                            })
                    }

                    AutoRunType.DAY_LOOP -> {
                        Log.i(
                            "每日任务",
                            "设置每日定时任务 | 时间: ${autoInfo.runTime!!.first}:${autoInfo.runTime!!.second}"
                        )
                        scheduler.addTask(
                            ScheduledTask(
                                autoInfo.title!!,
                                TaskSchedule.Daily(
                                    autoInfo.runTime!!.first,
                                    autoInfo.runTime!!.second
                                )
                            ) {
                                if (TimeUtils.isToday(autoInfo.todayRunTime.first)) {
                                    if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                                        Log.d(
                                            "任务执行",
                                            "今日已运行${autoInfo.todayRunTime.second}次，还需运行${autoInfo.runTimes - autoInfo.todayRunTime.second}次"
                                        )
                                        runLoop(
                                            autoInfo.runTimes - autoInfo.todayRunTime.second,
                                            autoInfo
                                        )
                                    } else {
                                        Log.d(
                                            "任务执行",
                                            "今日运行次数已达上限：${autoInfo.todayRunTime.second}"
                                        )
                                    }
                                } else {
                                    autoInfo.todayRunTime = Pair(0L, 0)
                                    autoInfoDao.update(autoInfo)
                                    Log.d("任务执行", "新的一天，重置运行次数")
                                    runLoop(autoInfo.runTimes, autoInfo)
                                }
                            })
                    }

                    AutoRunType.WEEK_LOOP -> {
                        Log.i(
                            "每周任务",
                            "设置每周定时任务 | 星期: ${autoInfo.weekData} | 时间: ${autoInfo.runTime!!.first}:${autoInfo.runTime!!.second}"
                        )
                        scheduler.addTask(
                            ScheduledTask(
                                autoInfo.title!!,
                                TaskSchedule.Weekly(
                                    autoInfo.weekData,
                                    autoInfo.runTime!!.first,
                                    autoInfo.runTime!!.second
                                )
                            ) {
                                if (TimeUtils.isToday(autoInfo.todayRunTime.first)) {
                                    if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                                        Log.d(
                                            "任务执行",
                                            "今日已运行${autoInfo.todayRunTime.second}次，还需运行${autoInfo.runTimes - autoInfo.todayRunTime.second}次"
                                        )
                                        runLoop(
                                            autoInfo.runTimes - autoInfo.todayRunTime.second,
                                            autoInfo,
                                            autoInfo.todayRunTime.second
                                        )
                                    } else {
                                        Log.d("任务执行", "今日运行次数已达上限")
                                    }
                                } else {
                                    autoInfo.todayRunTime = Pair(0L, 0)
                                    autoInfoDao.update(autoInfo)
                                    Log.d("任务执行", "新的一天，重置运行次数")
                                    runLoop(autoInfo.runTimes, autoInfo)
                                }
                            })
                    }
                }
            }
        }
    }

    private suspend fun runLoop(times: Int, autoInfo: AutoInfo, alreadyTimes: Int = 0) {
        var index = 0
        Log.i("任务循环", "开始执行任务循环 | 任务: ${autoInfo.title} | 总次数: $times")
        while (times == -1 || index < times) {
            saveToast(autoInfo, "剩余次数：${times - index}}")
            // 更新状态
            onStatusUpdate?.invoke(
                autoInfo.title ?: "未知任务",
                autoInfo.loopType,
                index + alreadyTimes + 1,
                times + alreadyTimes
            )

            if (TimeUtils.isInCurrentTime(autoInfo.runTime!!)) {
                if (autoInfo.runState && accessibilityService?.isAppForeground(autoInfo.runPackageName!!) == false) {
                    if (index == 0) {
                        Log.d("应用启动", "尝试打开应用: ${autoInfo.runAppName}")
                        AppUtils.openApp(MyApp.instance, autoInfo.runPackageName!!)
                        delay(5000)
                    }
                } else if (!autoInfo.runState && accessibilityService?.isAppForeground(autoInfo.runPackageName!!) == false) {
                    Log.d("应用状态", "应用未在前台且不允许运行，终止任务")
                    return
                }

                autoInfo.runInfo?.forEach { runBean ->
                    Log.d(
                        "任务步骤",
                        "执行任务步骤: ${runBean.clickBean?.clickType ?: runBean.triggerBean?.triggerType}"
                    )
                    runAuto(runBean)
                }
            } else {
                Log.d("时间检查", "当前时间不在任务执行时间范围内")
                return
            }

            val runTimes = autoInfo.todayRunTime.second + 1
            autoInfo.todayRunTime = Pair(System.currentTimeMillis(), runTimes)
            autoInfoDao.update(autoInfo)
            index++
            Log.d("任务进度", "任务完成一次循环 | 已完成: $index/$times")
        }
        saveToast(autoInfo, "剩余次数：0")
        Log.i("任务循环", "任务循环执行完成 | 任务: ${autoInfo.title}")
        onStatusUpdate?.invoke("", null, 0, 0) // 重置状态
    }

    private suspend fun runAuto(runBean: RunBean) {
        when {
            runBean.clickBean != null -> {
                Log.d("动作执行", "执行点击动作: ${runBean.clickBean!!.clickType}")
                runTask(runBean.clickBean!!)
            }

            runBean.triggerBean != null -> {
                Log.d("触发器执行", "执行触发器: ${runBean.triggerBean!!.triggerType}")
                runTrigger(runBean.triggerBean!!)
            }
        }
    }

    private suspend fun runTrigger(triggerBean: TriggerBean) {
        var isLoop = true
        val endTime = System.currentTimeMillis() + triggerBean.runScanTime * 1000L
        Log.i(
            "触发器",
            "开始执行触发器 | 类型: ${triggerBean.triggerType} | 扫描时间: ${triggerBean.runScanTime}秒"
        )

        while (System.currentTimeMillis() < endTime && isLoop) {
            if (System.currentTimeMillis() >= endTime) {
                isLoop = false
                Log.d("触发器", "扫描时间结束")
            }

            when (triggerBean.triggerType) {
                RuleType.TIME -> {
                    Log.d("时间触发器", "检查时间条件")
                    if (TimeUtils.isInCurrentTime(triggerBean.runTime!!)) {
                        Log.d("时间触发器", "时间条件满足")
                        triggerBean.runTrueTask?.let {
                            Log.d("时间触发器", "执行成功任务")
                            runTask(it)
                        }
                        triggerBean.runTrueAuto?.second?.forEach { runBean ->
                            Log.d("时间触发器", "执行成功自动化任务")
                            runAuto(runBean)
                        }
                        break
                    }
                }

                RuleType.FIND_TEXT -> {
                    Log.d("文本触发器", "查找文本: ${triggerBean.findText}")
                    if (triggerBean.isFindText4Node) {
                        if (accessibilityService != null) {
                            val textFound = accessibilityService!!.findText(triggerBean.findText!!)
                            Log.d("文本触发器", "节点查找结果: ${textFound.first}")
                            if (textFound.first) {
                                triggerBean.runTrueTask?.let {
                                    Log.d("文本触发器", "执行成功任务")
                                    runTask(it)
                                }
                                triggerBean.runTrueAuto?.second?.forEach { runBean ->
                                    Log.d("文本触发器", "执行成功自动化任务")
                                    runAuto(runBean)
                                }
                                break
                            }
                        } else {
                            Log.e("无障碍服务", "无障碍服务未激活")
                        }
                    } else {
                        Log.d("OCR触发器", "通过OCR查找文本")
                        val nodeResult = triggerBean.findText?.let {
                            AutoRunManager.findText(
                                it,
                                triggerBean.findTextType,
                                triggerBean.runScanTime
                            )
                        }
                        if (nodeResult?.first == true) {
                            Log.d("OCR触发器", "文本找到，执行成功分支")
                            triggerBean.runTrueTask?.let { runTask(it) }
                            triggerBean.runTrueAuto?.second?.forEach { runAuto(it) }
                        } else {
                            Log.d("OCR触发器", "文本未找到，执行失败分支")
                            triggerBean.runFalseTask?.let { runTask(it) }
                            triggerBean.runFalseAuto?.second?.forEach { runAuto(it) }
                        }
                    }
                }

                else -> {}
            }
        }

        if (!isLoop) {
            Log.d("触发器", "触发器超时未满足条件")
            triggerBean.runFalseTask?.let {
                Log.d("触发器", "执行失败任务")
                runTask(it)
            }
            triggerBean.runFalseAuto?.second?.forEach {
                Log.d("触发器", "执行失败自动化任务")
                runAuto(it)
            }
        }
    }

    private suspend fun runTask(clickBean: ClickBean) {
        Log.i(
            "任务动作",
            "开始执行动作 | 类型: ${clickBean.clickType} | 循环次数: ${clickBean.loopTimes}"
        )
        repeat(clickBean.loopTimes) {
            when (clickBean.clickType!!) {
                RunType.CLICK_XY -> {
                    Log.d(
                        "点击动作",
                        "坐标点击: (${clickBean.clickXy.first}, ${clickBean.clickXy.second})"
                    )
                    click(clickBean.clickXy.first, clickBean.clickXy.second)
                }

                RunType.ENTER_TEXT -> {
                    Log.d("文本输入", "输入文本: ${clickBean.enterText}")
                    if (accessibilityService != null) {
                        val textEntered =
                            clickBean.enterText?.let { accessibilityService!!.enterText(it) }
                        Log.d("文本输入", "输入结果: $textEntered")
                    } else {
                        Log.e("无障碍服务", "无障碍服务未激活")
                    }
                }

                RunType.CLICK_TEXT -> {
                    Log.d("文本点击", "尝试点击文本: ${clickBean.text}")
                    if (clickBean.isFindText4Node) {
                        if (accessibilityService != null) {
                            val clickNode = clickBean.text?.let {
                                accessibilityService!!.clickText(it, clickBean.findTextType)
                            }
                            Log.d("节点点击", "点击结果: $clickNode")
                        } else {
                            Log.e("无障碍服务", "无障碍服务未激活")
                        }
                    } else {
                        Log.d("OCR点击", "通过OCR查找文本并点击")
                        val nodeResult = clickBean.text?.let {
                            AutoRunManager.findText(
                                it,
                                clickBean.findTextType,
                                clickBean.findTextTime
                            )
                        }
                        if (nodeResult?.first == true) {
                            val rect = nodeResult.second!!.boundingBox!!
                            Log.d(
                                "OCR点击",
                                "找到文本位置，点击中心点: (${rect.centerX()}, ${rect.centerY()})"
                            )
                            click(rect.centerX(), rect.centerY())
                        }
                    }
                }

                RunType.LONG_VEH, RunType.LONG_HOR -> {
                    val runType = if (clickBean.clickType == RunType.LONG_HOR) {
                        Log.d(
                            "滑动动作",
                            "开始水平滑动 | 时间: ${clickBean.scrollTime}秒 | 范围: ${clickBean.scrollMinMax}"
                        )
                        "horizontal"
                    } else {
                        Log.d(
                            "滑动动作",
                            "开始垂直滑动 | 时间: ${clickBean.scrollTime}秒 | 范围: ${clickBean.scrollMinMax}"
                        )
                        "vertical"
                    }
                    startSliding(
                        runType,
                        clickBean.clickXy.first,
                        clickBean.clickXy.second,
                        clickBean.scrollMinMax.first,
                        clickBean.scrollMinMax.second,
                        clickBean.scrollTime * 1000L
                    )
                }

                RunType.SCROLL_LEFT -> {
                    Log.d("滑动动作", "执行左滑动作")
                    swipe(800, 1400, 400, 1400)
                }

                RunType.SCROLL_RIGHT -> {
                    Log.d("滑动动作", "执行右滑动作")
                    swipe(400, 1400, 800, 1400)
                }

                RunType.SCROLL_TOP -> {
                    Log.d("滑动动作", "执行上滑动作")
                    swipe(600, 1200, 600, 800)
                }

                RunType.SCROLL_BOTTOM -> {
                    Log.d("滑动动作", "执行下滑动作")
                    swipe(600, 800, 600, 1200)
                }

                RunType.TASK -> {
                    Log.i("子任务", "开始执行子任务: ${clickBean.runTask?.first}")
                    clickBean.runTask?.second?.forEach { runBean ->
                        runAuto(runBean)
                    }
                }

                RunType.GO_BACK -> {
                    Log.d("返回动作", "执行返回操作")
                    back()
                }

                RunType.OPEN_APP -> {
                    Log.d("应用启动", "尝试打开应用: ${clickBean.openAppData?.first}")
                    clickBean.openAppData?.second?.let {
                        AppUtils.openApp(MyApp.instance, it)
                    }
                }

                RunType.LONG_CLICK -> {
                    Log.d(
                        "长按动作",
                        "执行长按: (${clickBean.clickXy.first}, ${clickBean.clickXy.second}) | 时长: ${clickBean.longClickTime}秒"
                    )
                    click(
                        clickBean.clickXy.first,
                        clickBean.clickXy.second,
                        duration = clickBean.longClickTime * 1000L
                    )
                }
            }
            Log.d("动作间隔", "等待${clickBean.sleepTime}秒后执行下一个动作")
            delay(clickBean.sleepTime * 1000L)
        }
        Log.i("任务动作", "动作执行完成")
    }

    private fun saveToast(autoInfo: AutoInfo, toast: String) {
        FloatingWindowService.toastTip?.value = ""
    }
}