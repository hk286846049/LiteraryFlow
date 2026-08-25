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
import com.monster.literaryflow.service.AlarmService
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import taylor.com.util.Preference
import kotlin.coroutines.cancellation.CancellationException


class CustomTask(val context: Context, val autoInfoDao: AutoInfoDao) {
    private var taskJob: Job? = null
    private val _state = MutableStateFlow<TaskState>(TaskState.Idle)
    private var accessibilityService: MyAccessibilityService? = null
    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var scheduler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        TaskScheduler()
    } else {
        TODO("VERSION.SDK_INT < O")
    }
    var onStatusUpdate: ((taskName: String, loopType: AutoRunType?, progress: Int, total: Int) -> Unit)? =
        null
    var onTaskFinish: (() -> Unit)? = null
    private val TAG = "【任务调度】"
    private val TAG2 = "【任务执行】"


    private var isRestart = false
    
    // 任务状态枚举
    sealed class TaskState {
        object Idle : TaskState()
        object Running : TaskState()
        object Paused : TaskState()
        object Cancelled : TaskState()
    }


    fun pause() {
        Log.i(TAG, "====== 暂停任务 ======")
        _state.value = TaskState.Paused
    }
    fun resume() {
        Log.i(TAG, "====== 恢复任务 ======")
        _state.value = TaskState.Running
    }

    // 发送任务数据并开始执行
    fun sendData(data: List<AutoInfo>, restart: Boolean = false) {
        accessibilityService = FastAccessibilityService.instance as? MyAccessibilityService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scheduler = TaskScheduler()
        }
        _state.value = TaskState.Running
        if (_state.value != TaskState.Cancelled) {
            Log.d(TAG, "分发新任务数据到通道")
            taskJob = CoroutineScope(Dispatchers.IO).launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runAuto(data)
                }
            }
            isRestart = restart
        }
    }

    // 执行自动化任务(Android O及以上)
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun runAuto(list: List<AutoInfo>?) {
        if (list == null) return
        Log.i(TAG2, "====== 开始执行自动任务 ======")
        list.forEach { autoInfo ->
            // 检查是否需要启动录屏服务
            if (MyApp.imageReader == null && autoInfo.runInfo?.find {
                    it.clickBean?.isFindText4Node == false || it.triggerBean.find { !it.isFindText4Node } != null
                } != null) {
                // 录屏相关设置和启动逻辑...
                startScreenRecoding(autoInfo)
            } else {
                Log.d(TAG2, "MyApp.imageReader: ${MyApp.imageReader}")
            }
            Log.i(TAG2, "处理任务: ${autoInfo.title} | 类型: ${autoInfo.loopType}")
            // 根据任务类型执行不同逻辑
            when (autoInfo.loopType) {
                AutoRunType.SPECIFIED_NUMBER -> {
                    if (TimeUtils.isToday(autoInfo.todayRunTime.first) || isRestart) {
                        if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                            Log.d(
                                TAG2,
                                "今日已运行${autoInfo.todayRunTime.second}次，还需运行${autoInfo.runTimes - autoInfo.todayRunTime.second}次"
                            )
                            runLoop(autoInfo.runTimes - autoInfo.todayRunTime.second, autoInfo)
                        } else {
                            Log.d(TAG2, "今日运行次数已达上限")
                        }
                    } else {
                        autoInfo.todayRunTime = Pair(0L, 0)
                        autoInfoDao.update(autoInfo)
                        Log.d(TAG2, "新的一天，重置运行次数")
                        runLoop(autoInfo.runTimes, autoInfo)
                    }
                }

                AutoRunType.LOOP -> {
                    Log.i(TAG2, "设置循环任务 | 间隔: ${autoInfo.sleepTime}秒")
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
                        TAG2,
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
                            if (TimeUtils.isToday(autoInfo.todayRunTime.first) || isRestart) {
                                if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                                    Log.d(
                                        TAG2,
                                        "今日已运行${autoInfo.todayRunTime.second}次，还需运行${autoInfo.runTimes - autoInfo.todayRunTime.second}次"
                                    )
                                    runLoop(
                                        autoInfo.runTimes - autoInfo.todayRunTime.second,
                                        autoInfo
                                    )
                                } else {
                                    Log.d(
                                        TAG2,
                                        "今日运行次数已达上限：${autoInfo.todayRunTime.second}"
                                    )
                                }
                            } else {
                                autoInfo.todayRunTime = Pair(0L, 0)
                                autoInfoDao.update(autoInfo)
                                Log.d(TAG2, "新的一天，重置运行次数")
                                runLoop(autoInfo.runTimes, autoInfo)
                            }

                        })
                }

                AutoRunType.WEEK_LOOP -> {
                    Log.i(
                        TAG2,
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
                            if (TimeUtils.isToday(autoInfo.todayRunTime.first)|| isRestart) {
                                if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                                    Log.d(
                                        TAG2,
                                        "今日已运行${autoInfo.todayRunTime.second}次，还需运行${autoInfo.runTimes - autoInfo.todayRunTime.second}次"
                                    )
                                    runLoop(
                                        autoInfo.runTimes - autoInfo.todayRunTime.second,
                                        autoInfo,
                                        autoInfo.todayRunTime.second
                                    )
                                } else {
                                    Log.d(TAG2, "今日运行次数已达上限")
                                }
                            } else {
                                autoInfo.todayRunTime = Pair(0L, 0)

                                autoInfoDao.update(autoInfo)
                                Log.d(TAG2, "新的一天，重置运行次数")
                                runLoop(autoInfo.runTimes, autoInfo)
                            }
                        })
                }

                else -> {}
            }
        }

    }

    private suspend fun startScreenRecoding(autoInfo: AutoInfo) {
        val nowApp = autoInfo.runPackageName
        val landscapePrefs = context.getSharedPreferences(
            "landscape_apps",
            AppCompatActivity.MODE_PRIVATE
        )
        val landscapePreferences = Preference(landscapePrefs)
        val landscapeApps: Set<String> =
            landscapePreferences["landscape_apps", emptySet()]
        Log.d(TAG2, "landscapeApps = $landscapeApps")
        val isGame = landscapeApps.contains(nowApp)
        AppUtils.openApp(MyApp.instance, "com.monster.literaryflow")
        delay(2000)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        delay(1000)
        if (isGame) {
            SharedData.trigger.postValue("打开录屏|横屏")
        } else {
            SharedData.trigger.postValue("打开录屏|竖屏")
        }
        delay(500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (accessibilityService != null) {
                accessibilityService!!.clickText("单个应用", TextPickType.EXACT_MATCH,2000)
                delay(1000)
                accessibilityService!!.clickText("整个屏幕", TextPickType.EXACT_MATCH,2000)
                delay(1000)
                accessibilityService!!.clickText("立即开始", TextPickType.EXACT_MATCH,2000)
                delay(1000)
            } else {
                Log.e(TAG2, "无障碍服务未激活")
            }
        } else {
            if (accessibilityService != null) {
                accessibilityService!!.clickText("立即开始", TextPickType.EXACT_MATCH,2000)
                delay(1000)
            } else {
                Log.e(TAG2, "无障碍服务未激活")
            }
        }
        nowApp?.let { AppUtils.openApp(MyApp.instance, it) }
    }

    // 执行任务循环
    private suspend fun runLoop(times: Int, autoInfo: AutoInfo, alreadyTimes: Int = 0) {
        var index = 0
        Log.i(TAG2, "开始执行任务循环 | 任务: ${autoInfo.title} | 总次数: $times")
        while (times == -1 || index < times) {
            saveToast(autoInfo, "剩余次数：${times - index}}")
            // 更新状态
            onStatusUpdate?.invoke(
                autoInfo.title ?: "未知任务",
                autoInfo.loopType,
                index + alreadyTimes + 1,
                times + alreadyTimes
            )
            // 检查任务执行时间
            if (TimeUtils.isInCurrentTime(autoInfo.runTime!!)) {
                // 检查应用是否在前台
                if (autoInfo.runState && accessibilityService?.isAppForeground(autoInfo.runPackageName!!) == false) {
                    if (index == 0) {
                        Log.d(TAG2, "尝试打开应用: ${autoInfo.runAppName}")
                        AppUtils.openApp(MyApp.instance, autoInfo.runPackageName!!)
                        delay(5000)
                    }
                } else if (!autoInfo.runState && accessibilityService?.isAppForeground(autoInfo.runPackageName!!) == false) {
                    Log.d(TAG2, "应用未在前台且不允许运行，终止任务")
                    return
                }

                autoInfo.runInfo?.forEach { runBean ->
                    Log.d(
                        TAG2,
                        "执行任务步骤: ${runBean.clickBean?.clickType ?: runBean.triggerBean}"
                    )
                    runAuto(runBean)
                }
            } else {
                Log.d(TAG2, "当前时间不在任务执行时间范围内")
                return
            }

            // 更新运行次数
            val runTimes = autoInfo.todayRunTime.second + 1
            autoInfo.todayRunTime = Pair(System.currentTimeMillis(), runTimes)
            if (!isRestart) {
                autoInfoDao.update(autoInfo)
            }
            index++
            Log.d(TAG2, "任务完成一次循环 | 已完成: $index/$times")
        }
        Log.i(TAG2, "任务循环执行完成 | 任务: ${autoInfo.title}")
        onStatusUpdate?.invoke("", null, 0, 0) // 重置状态
    }

    // 执行单个任务步骤
    private suspend fun runAuto(runBean: RunBean) {
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        when {
            runBean.clickBean != null -> {
                Log.d(TAG2, "执行点击动作: ${runBean.clickBean!!.clickType}")
                runTask(runBean.clickBean!!)
            }

            runBean.triggerBean != null -> {
                Log.d(TAG2, "执行触发器: ${runBean.triggerBean!!}")
                runTrigger(runBean.triggerBean!!)
            }
        }
    }

    // 执行触发器逻辑
    private suspend fun runTrigger(triggerBeans: List<TriggerBean>) = coroutineScope {
        triggerBeans.map { bean ->
            launch {
                runTrigger(bean)
            }
        }.joinAll()
    }
    // 执行单个触发器
    private suspend fun runTrigger(triggerBean: TriggerBean) {
        var isLoop = true
        val endTime = System.currentTimeMillis() + triggerBean.runScanTime * 1000L
        Log.i(
            TAG2,
            "开始执行触发器 | 类型: ${triggerBean.triggerType} | 扫描时间: ${triggerBean.runScanTime}秒"
        )
        try {
            if(!scope.isActive){
                Log.d(TAG2, "任务已被取消")
            }
            while (System.currentTimeMillis() < endTime && isLoop && scope.isActive) {
                if (System.currentTimeMillis() >= endTime) {
                    isLoop = false
                    Log.d(TAG2, "扫描时间结束")
                }

                when (triggerBean.triggerType) {
                    RuleType.TIME -> {
                        Log.d(TAG2, "检查时间条件")
                        if (TimeUtils.isInCurrentTime(triggerBean.runTime!!)) {
                            Log.d(TAG2, "时间条件满足")
                            triggerBean.runTrueTask?.let {
                                Log.d(TAG2, "执行成功任务")
                                runTask(it)
                            }
                            triggerBean.runTrueAuto?.second?.forEach { runBean ->
                                Log.d(TAG2, "执行成功自动化任务")
                                runAuto(runBean)
                            }
                            scope.cancel("条件满足，取消其他任务")
                            break
                        }
                    }

                    RuleType.FIND_TEXT -> {
                        Log.d(TAG2, "查找文本: ${triggerBean.findText}")
                        if (triggerBean.isFindText4Node) {
                            if (accessibilityService != null) {
                                val isFound =
                                    accessibilityService!!.findText(triggerBean.findText!!,triggerBean.findTextType,triggerBean.runScanTime)
                                Log.d(TAG2, "节点查找结果: ${isFound}")
                                if (isFound) {
                                    triggerBean.runTrueTask?.let {
                                        Log.d(TAG2, "执行成功任务")
                                        runTask(it)
                                    }
                                    triggerBean.runTrueAuto?.second?.forEach { runBean ->
                                        Log.d(TAG2, "执行成功自动化任务")
                                        runAuto(runBean)
                                    }
                                    scope.cancel("条件满足，取消其他任务")
                                    break
                                }
                            } else {
                                Log.e(TAG2, "无障碍服务未激活")
                            }
                        } else {
                            Log.d(TAG2, "通过OCR查找文本")
                            val nodeResult = triggerBean.findText?.let {
                                AutoRunManager.findText(
                                    it,
                                    triggerBean.findTextType,
                                    triggerBean.runScanTime
                                )
                            }
                            if (nodeResult?.first == true) {
                                Log.d(TAG2, "文本找到，执行成功分支")
                                triggerBean.runTrueTask?.let { runTask(it) }
                                triggerBean.runTrueAuto?.second?.forEach { runAuto(it) }
                                scope.cancel("条件满足，取消其他任务")
                            } else {
                                Log.d(TAG2, "文本未找到，执行失败分支")
                                triggerBean.runFalseTask?.let { runTask(it) }
                                triggerBean.runFalseAuto?.second?.forEach { runAuto(it) }
                            }
                        }
                    }

                    else -> {}
                }
                delay(100) // 添加小的延迟避免过于频繁的检查
            }

            if (!isLoop && scope.isActive) {
                Log.d(TAG2, "触发器超时未满足条件")
                triggerBean.runFalseTask?.let {
                    Log.d(TAG2, "执行失败任务")
                    runTask(it)
                }
                triggerBean.runFalseAuto?.second?.forEach {
                    Log.d(TAG2, "执行失败自动化任务")
                    runAuto(it)
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG2, "触发器任务被取消: ${e.message}")
        }
    }

    // 执行具体任务动作
    private suspend fun runTask(clickBean: ClickBean) {
        Log.i(
            TAG2,
            "开始执行动作 | 类型: ${clickBean.clickType} | 循环次数: ${clickBean.loopTimes}"
        )
        repeat(clickBean.loopTimes) {
            if (!isActive) throw CancellationException()
            when (clickBean.clickType!!) {
                RunType.CLICK_XY -> {
                    Log.d(
                        TAG2,
                        "坐标点击: (${clickBean.clickXy.first}, ${clickBean.clickXy.second})"
                    )
                    click(clickBean.clickXy.first, clickBean.clickXy.second)
                }

                RunType.ENTER_TEXT -> {
                    Log.d(TAG2, "输入文本: ${clickBean.enterText}")
                    if (accessibilityService != null) {
                        val textEntered =
                            clickBean.enterText?.let { accessibilityService!!.enterText(it) }
                        Log.d(TAG2, "输入结果: $textEntered")
                    } else {
                        Log.e(TAG2, "无障碍服务未激活")
                    }
                }

                RunType.CLICK_TEXT -> {
                    Log.d(TAG2, "尝试点击文本: ${clickBean.text}")
                    if (clickBean.isFindText4Node) {
                        if (accessibilityService != null) {
                            val clickNode = clickBean.text?.let {
                                accessibilityService!!.clickText(it, clickBean.findTextType,clickBean.findTextTime)
                            }
                            Log.d(TAG2, "点击结果: $clickNode")
                        } else {
                            Log.e(TAG2, "无障碍服务未激活")
                        }
                    } else {
                        Log.d(TAG2, "通过OCR查找文本并点击")
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
                                TAG2,
                                "找到文本位置，点击中心点: (${rect.centerX()}, ${rect.centerY()})"
                            )
                            click(rect.centerX(), rect.centerY())
                        }
                    }
                }

                RunType.LONG_VEH, RunType.LONG_HOR -> {
                    val runType = if (clickBean.clickType == RunType.LONG_HOR) {
                        Log.d(
                            TAG2,
                            "开始水平滑动 | 时间: ${clickBean.scrollTime}秒 | 范围: ${clickBean.scrollMinMax}"
                        )
                        "horizontal"
                    } else {
                        Log.d(
                            TAG2,
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
                    Log.d(TAG2, "执行左滑动作")
                    swipe(800, 1400, 400, 1400)
                }

                RunType.SCROLL_RIGHT -> {
                    Log.d(TAG2, "执行右滑动作")
                    swipe(400, 1400, 800, 1400)
                }

                RunType.SCROLL_TOP -> {
                    Log.d(TAG2, "执行上滑动作")
                    swipe(600, 1200, 600, 800)
                }

                RunType.SCROLL_BOTTOM -> {
                    Log.d(TAG2, "执行下滑动作")
                    swipe(600, 800, 600, 1200)
                }

                RunType.TASK -> {
                    Log.i(TAG2, "开始执行子任务: ${clickBean.runTask?.first}")

                    clickBean.runTask?.second?.forEach { runBean ->
                        runAuto(runBean)
                    }
                }

                RunType.GO_BACK -> {
                    Log.d(TAG2, "执行返回操作")
                    back()
                }

                RunType.OPEN_APP -> {
                    Log.d(TAG2, "尝试打开应用: ${clickBean.openAppData?.first}")
                    clickBean.openAppData?.second?.let {
                        AppUtils.openApp(MyApp.instance, it)
                    }
                }

                RunType.LONG_CLICK -> {
                    Log.d(
                        TAG2,
                        "执行长按: (${clickBean.clickXy.first}, ${clickBean.clickXy.second}) | 时长: ${clickBean.longClickTime}秒"
                    )
                    click(
                        clickBean.clickXy.first,
                        clickBean.clickXy.second,
                        duration = clickBean.longClickTime * 1000L
                    )
                }
            }
            Log.d(TAG2, "等待${clickBean.sleepTime}秒后执行下一个动作")
            delay(clickBean.sleepTime * 1000L)
        }
        Log.i(TAG2, "动作执行完成")

    }

    private fun saveToast(autoInfo: AutoInfo, toast: String) {
        FloatingWindowService.toastTip?.value = ""
    }
    // 取消所有任务
    fun cancel() {
        Log.i(TAG, "====== 取消所有任务 ======")
        _state.value =TaskState.Cancelled
        taskJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scheduler.cancelAll()
        }
        // 新增无障碍服务操作取消
        accessibilityService?.apply {
            stopAllFetchingNodes()  // 停止所有节点获取操作
            fetchHandler?.removeCallbacksAndMessages(null)  // 清除Handler回调
            fetchScheduler?.shutdownNow()  // 关闭定时线程池
        }
        stopSliding()
        accessibilityService = null
    }
}