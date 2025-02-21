/*
package com.monster.literaryflow.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import cn.coderpig.cp_fast_accessibility.NodeWrapper
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.startSliding
import cn.coderpig.cp_fast_accessibility.swipe
import com.benjaminwan.ocrlibrary.OcrResult
import com.benjaminwan.ocrlibrary.TextBlock
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.room.AutoInfoDao
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.max
class TaskScheduler(
    private val context: Context,
    private val autoInfoDao: AutoInfoDao
) {
    private val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    private val runningTasks = ConcurrentHashMap<Int, CustomTask>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun scheduleDailyTask(autoInfo: AutoInfo) {
        // 设置每日定时
        val triggerTime = calculateNextTriggerTime(autoInfo.runTime!!.first)
        val intent = Intent(context, TaskReceiver::class.java).apply {
            putExtra("TASK_ID", autoInfo.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            autoInfo.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun calculateNextTriggerTime(runTime: Pair<Int, Int>): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, runTime.first)
            set(Calendar.MINUTE, runTime.second)
            set(Calendar.SECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    // 修改后的任务执行入口
    fun executeTask(taskId: Int) {
        coroutineScope.launch {
            val autoInfo = autoInfoDao.getById(taskId)
            autoInfo?.takeIf { it.isRun }?.let {
                val task = runningTasks.getOrPut(taskId) {
                    CustomTask(autoInfoDao).apply {
                        start()
                    }
                }

                // 并发控制
                if (task.state.value != TaskState.Running) {
                    task.resume()
                }

                // 注入任务数据
                task.sendData(listOf(it))
            }
        }
    }

    fun pauseTask(taskId: Int) {
        runningTasks[taskId]?.pause()
    }

    fun stopTask(taskId: Int) {
        runningTasks[taskId]?.cancel()
        runningTasks.remove(taskId)
        cancelAlarm(taskId)
    }

    private fun cancelAlarm(taskId: Int) {
        val intent = Intent(context, TaskReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

// 修改后的CustomTask增强版
class EnhancedCustomTask(
    private val autoInfoDao: AutoInfoDao,
    private val taskScheduler: TaskScheduler
) : CustomTask(autoInfoDao) {

    // 增强的runLoop方法
    private suspend fun enhancedRunLoop(times: Int, autoInfo: AutoInfo) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            runLoop(times, autoInfo)
        }

        // 自动重置每日任务
        if (times > 0) {
            autoInfo.todayRunTime = Pair(System.currentTimeMillis(), times)
            autoInfoDao.update(autoInfo)
            taskScheduler.scheduleDailyTask(autoInfo)
        }

        job.join()
    }

    // 优化后的任务执行流程
    override suspend fun runAuto(list: List<AutoInfo>?) {
        if (list == null) return

        list.forEach { autoInfo ->
            when {
                TimeUtils.isToday(autoInfo.todayRunTime.first) -> {
                    handleDailyExecution(autoInfo)
                }
                else -> {
                    handleNewDayExecution(autoInfo)
                }
            }
        }
    }

    private suspend fun handleDailyExecution(autoInfo: AutoInfo) {
        val remaining = autoInfo.runTimes - autoInfo.todayRunTime.second
        if (remaining > 0) {
            Log.d("Scheduler", "${autoInfo.title} 继续执行剩余次数: $remaining")
            enhancedRunLoop(remaining, autoInfo)
        }
    }

    private suspend fun handleNewDayExecution(autoInfo: AutoInfo) {
        autoInfo.todayRunTime = Pair(0L, 0)
        autoInfoDao.update(autoInfo)
        Log.d("Scheduler", "${autoInfo.title} 新的一天开始执行")
        enhancedRunLoop(autoInfo.runTimes, autoInfo)
    }
}

// 定时任务接收器（新增）
class TaskReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val scheduler = TaskScheduler(context, DatabaseProvider.getAutoInfoDao())
        scheduler.executeTask(taskId)
    }
}

// 设备重启监听（新增）
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val activeTasks = DatabaseProvider.getAutoInfoDao().getActiveTasks()
                val scheduler = TaskScheduler(context, DatabaseProvider.getAutoInfoDao())
                activeTasks.forEach { scheduler.scheduleDailyTask(it) }
            }
        }
    }
}

// 状态管理增强
sealed class TaskState {
    object Idle : TaskState()
    object Running : TaskState()
    object Paused : TaskState()
    object Cancelled : TaskState()
    data class Error(val message: String) : TaskState()
}

// 并发控制优化
class ConcurrentExecutor {
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)
    private val dispatcher = executor.asCoroutineDispatcher()

    fun <T> executeAsync(block: suspend () -> T): Deferred<T> {
        return CoroutineScope(dispatcher).async { block() }
    }
}



class CustomTask(private val autoInfoDao: AutoInfoDao) {
    private val taskChannel = Channel<List<AutoInfo>>(Channel.UNLIMITED) // 用于传递任务数据
    private var taskJob: Job? = null
    private val _state = MutableStateFlow<TaskState>(TaskState.Idle)
    val state = _state.asStateFlow() // 对外暴露的只读状态流

    private var accessibilityService: MyAccessibilityService? = null
    private var imgJob: Job? = null
    private val _imgState = MutableStateFlow<TaskState>(TaskState.Idle)

    // 启动任务
    fun start() {
        if (_state.value == TaskState.Running) return
        accessibilityService = FastAccessibilityService.instance as? MyAccessibilityService
        _state.value = TaskState.Running
        taskJob = CoroutineScope(Dispatchers.IO).launch {
            for (data in taskChannel) {
                if (_state.value == TaskState.Paused) {
                    delay(100) // 等待状态切换
                    continue
                }
                if (_state.value == TaskState.Cancelled) break
                // 处理任务数据
                Log.d("FloatService", "${data.toString()}")
                process(data)
            }
        }
    }

    // 暂停任务
    fun pause() {
        if (_state.value == TaskState.Running) {
            _state.value = TaskState.Paused
        }
    }

    // 恢复任务
    fun resume() {
        if (_state.value == TaskState.Paused) {
            _state.value = TaskState.Running
        }
    }

    // 取消任务
    fun cancel() {
        _state.value = TaskState.Cancelled
        taskJob?.cancel()
        taskJob = null
    }

    // 重新传入数据
    fun sendData(data: List<AutoInfo>) {
        if (_state.value != TaskState.Cancelled) {
            taskChannel.trySend(data)
        }
    }

    // 处理任务数据
    private suspend fun process(data: List<AutoInfo>?) {
        Log.d("FloatService", "process")
        runAuto(data)
    }



    private suspend fun runAuto(list: List<AutoInfo>?) {
        if (list == null) return
        for (autoInfo in list) {
            Log.d("FloatService", "runAuto")
            if (autoInfo.isRun) {
                if (TimeUtils.isToday(autoInfo.todayRunTime.first)) {
                    if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                        Log.d(
                            "FloatService",
                            "${autoInfo.title} times: ${autoInfo.runTimes},alreadyRunTimes:${autoInfo.todayRunTime.second}"
                        )
                        runLoop(autoInfo.runTimes - autoInfo.todayRunTime.second, autoInfo)
                    } else {
                        Log.d("FloatService", "${autoInfo.title} run over")
                    }
                } else {
                    autoInfo.todayRunTime = Pair(0L, 0)
                    autoInfoDao.update(autoInfo)
                    Log.d("FloatService", "${autoInfo.title} newDay to run")
                    runLoop(autoInfo.runTimes, autoInfo)
                }
            }
        }
    }

    private suspend fun runLoop(times: Int, autoInfo: AutoInfo) {
        var index = 0
        while (index < times) {
            saveToast(autoInfo, "剩余次数：${times - index}}")
            if (TimeUtils.isInCurrentTime(autoInfo.runTime!!)) {
                if (autoInfo.runState && !AppUtils.isAppInForeground(
                        MyApp.instance,
                        autoInfo.runPackageName!!
                    )
                ) {
                    if (index == 0) {
                        Log.d("FloatService", "打开App:${autoInfo.runAppName}")
                        AppUtils.openApp(MyApp.instance, autoInfo.runPackageName!!)
                        delay(5000)
                    }
                } else if (!autoInfo.runState && !AppUtils.isAppInForeground(
                        MyApp.instance,
                        autoInfo.runPackageName!!
                    )
                ) {
                    return
                }
                for (runBean in autoInfo.runInfo!!) {
                    runAuto(runBean)
                }
            } else {
                return
            }
            val runTimes = autoInfo.todayRunTime.second + 1
            autoInfo.todayRunTime = Pair(System.currentTimeMillis(), runTimes)
            autoInfoDao.update(autoInfo)
            index++
        }
        saveToast(autoInfo, "剩余次数：0")
    }

    private suspend fun runAuto(runBean: RunBean){
        if (runBean.clickBean != null) {
            runTask(runBean.clickBean!!)
        } else if (runBean.triggerBean != null) {
            runTrigger(runBean.triggerBean!!)
        }
    }

    private suspend fun runTrigger(triggerBean: TriggerBean) {
        var isLoop = true
        val endTime =  System.currentTimeMillis() + triggerBean.runScanTime*1000L
        var nodeList:ArrayList<NodeWrapper>? = arrayListOf()
        while (System.currentTimeMillis()<endTime && isLoop){
            if (System.currentTimeMillis()>=endTime){
                isLoop = false
            }
            when (triggerBean.triggerType) {
                RuleType.TIME -> {
                    if (TimeUtils.isInCurrentTime(triggerBean.runTime!!)) {
                        if (triggerBean.runTrueTask!=null){
                            runTask(triggerBean.runTrueTask!!)
                        }else if (triggerBean.runTrueAuto!=null){
                            for (runBean in triggerBean.runTrueAuto!!.second!!){
                                runAuto(runBean)
                            }
                        }
                        break
                    }
                }
                RuleType.FIND_TEXT -> {
                    if (accessibilityService != null) {
                        // 检查是否存在文字
                        val textFound = accessibilityService!!.findText(triggerBean.findText!!)
                        Log.d("AnotherService", "Text found: $textFound")
                        if (textFound.first){
                            nodeList = null
                            if (triggerBean.runTrueTask!=null){
                                runTask(triggerBean.runTrueTask!!)
                            }else if (triggerBean.runTrueAuto!=null){
                                for (runBean in triggerBean.runTrueAuto!!.second!!){
                                    runAuto(runBean)
                                }
                            }
                            break
                        }else{
                            nodeList = textFound.second
                        }
                    } else {
                        Log.e("AnotherService", "MyAccessibilityService is not active")
                    }
                }
                else -> {}
            }

        }
        if (!isLoop){
            if (triggerBean.runFalseTask!=null){
                runTask(triggerBean.runFalseTask!!)
            }else if (triggerBean.runFalseAuto!=null){
                for (runBean in triggerBean.runFalseAuto!!.second!!){
                    runAuto(runBean)
                }
            }

        }
    }

    private suspend fun runTask(clickBean: ClickBean) {
        repeat(clickBean.loopTimes) {
            when (clickBean.clickType!!) {
                RunType.CLICK_XY -> {
                    click(
                        clickBean!!.clickXy.first,
                        clickBean.clickXy.second
                    )
                    Log.d(
                        "FloatService",
                        "点击:${clickBean.clickXy.first}，${clickBean.clickXy.second}"
                    )
                }

                RunType.ENTER_TEXT -> {
                    if (accessibilityService != null) {
                        // 输入文字
                        val textEntered =
                            clickBean.enterText?.let { it1 -> accessibilityService!!.enterText(it1) }
                        Log.d("AnotherService", "Text entered: $textEntered")
                    } else {
                        Log.e("AnotherService", "MyAccessibilityService is not active")
                    }
                }

                RunType.CLICK_TEXT -> {
                    if (accessibilityService != null) {
                        // 点击文字
                        val clickNode = clickBean.text?.let { it1 -> accessibilityService!!.clickText(it1) }
                        Log.d("AnotherService", "Text clicked: 【${clickBean.text}】$clickNode")
                    } else {
                        Log.e("AnotherService", "MyAccessibilityService is not active")
                    }

                    //截屏查找文字
                    */
/*   val startTime = System.currentTimeMillis()
                                    val ocrResult = startCaptureTask(
                                        clickBean!!.text!!,
                                        clickBean!!.findTextTime * 1000L,
                                        clickBean!!.findTextType
                                    )
                                    if (ocrResult.second) {
                                        Log.d(
                                            "FloatService",
                                            "查找耗时：${(System.currentTimeMillis() - startTime) / 1000}s"
                                        )
                                        val x = ocrResult.first?.let { it1 ->
                                            OcrTextUtils.findTextCenter(it1)
                                        }
                                        x?.let { it1 -> click(it1.first, x.second) }
                                    }*//*

                }

                RunType.LONG_VEH, RunType.LONG_HOR -> {
                    val runType =
                        if (clickBean!!.clickType == RunType.LONG_HOR) {
                            Log.d(
                                "FloatService",
                                "左右滑动${clickBean.scrollTime}秒"
                            )
                            "horizontal"
                        } else {
                            Log.d(
                                "FloatService",
                                "上下滑动${clickBean.scrollTime}秒"
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
                    swipe(800, 1400, 400, 1400)
                    Log.d("FloatService", "左滑")
                }

                RunType.SCROLL_RIGHT -> {
                    swipe(400, 1400, 800, 1400)
                    Log.d("FloatService", "右滑")
                }

                RunType.SCROLL_TOP -> {
                    swipe(600, 1200, 600, 800)
                    Log.d("FloatService", "上滑")
                }

                RunType.SCROLL_BOTTOM -> {
                    swipe(600, 800, 600, 1200)
                    Log.d("FloatService", "下滑")
                }
            }
            delay(clickBean.sleepTime * 1000L)
            Log.d("FloatService", "等待${clickBean.sleepTime}秒")
        }
    }

    */
/**
     * 启动任务，根据指定时间和目标内容寻找 OCR 结果。
     * @param targetContent 目标内容
     * @param searchDuration 搜索持续时间，单位：毫秒
     * @return 搜索结果，true 表示找到目标内容，false 表示未找到。
     *//*

    suspend fun startCaptureTask(
        targetContent: String,
        searchDuration: Long,
        findType: TextPickType
    ): Pair<TextBlock?, Boolean> {
        if (_imgState.value == TaskState.Running) return Pair(null, false) // 如果任务已在运行，直接返回

        _imgState.value = TaskState.Running

        val hotFlow = MutableSharedFlow<OcrResult>()
        val foundResult = CompletableDeferred<Pair<TextBlock?, Boolean>>() // 用于异步返回结果

        // 启动任务
        imgJob = CoroutineScope(Dispatchers.IO).launch {
            hotFlow.onEach { result ->
                val ocrResult = checkOcrResult(result, targetContent, findType)
                if (ocrResult.second) {
                    Log.d("FloatService", "发现目标文字")
                    foundResult.complete(ocrResult) // 找到目标内容
                    stopCaptureTask()
                }
            }.launchIn(this)

            val endTime = System.currentTimeMillis() + searchDuration
            while (_state.value != TaskState.Cancelled && System.currentTimeMillis() < endTime) {
                collectAndProcessImage(hotFlow)
                delay((1000L..1500L).random())
            }

            // 如果任务超时且未找到内容，则返回 false
            if (!foundResult.isCompleted) {
                foundResult.complete(Pair(null, false))
                stopCaptureTask()
            }
        }

        return foundResult.await()
    }

    */
/**
     * 停止任务。
     *//*

    private fun stopCaptureTask() {
        _imgState.value = TaskState.Cancelled
        imgJob?.cancel()
        imgJob = null
    }

    */
/**
     * 收集并处理图像。
     *//*

    private suspend fun collectAndProcessImage(hotFlow: MutableSharedFlow<OcrResult>) {
        try {
            val image = MyApp.imageReader?.acquireLatestImage()
            image?.use { acquiredImage ->

                val bitmap = ImageUtils.imageToBitmap(acquiredImage)
                MyApp.image = bitmap
                val maxSize = max(bitmap.width, bitmap.height)
                val boxImg =
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

                val ocrResult = MyApp.ocrEngine?.detect(bitmap, boxImg, maxSize)
                ocrResult?.let { hotFlow.emit(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    */
/**
     * 检查 OCR 结果中是否包含目标内容。
     * @param result OCR 结果
     * @param targetContent 目标内容
     * @return 是否找到目标内容
     *//*

    private fun checkOcrResult(
        result: OcrResult,
        targetContent: String,
        findType: TextPickType
    ): Pair<TextBlock?, Boolean> {
        Log.d("#####MONSTER#####", "Checking OCR result: ${result.textBlocks}")
        var resultValue: Pair<TextBlock?, Boolean> = Pair(null, false)
        when (findType) {
            TextPickType.EXACT_MATCH -> {
                resultValue = if (result.textBlocks.find { it.text == targetContent } != null) {
                    Pair(result.textBlocks.find { it.text == targetContent }, true)
                } else {
                    Pair(null, false)
                }
            }

            TextPickType.FUZZY_MATCH -> {
                resultValue =
                    if (result.textBlocks.find { it.text.contains(targetContent) } != null) {
                        Pair(result.textBlocks.find { it.text.contains(targetContent) }, true)
                    } else {
                        Pair(null, false)
                    }

            }

            TextPickType.MULTIPLE_FUZZY_WORDS -> {
                val resultList = targetContent.split("#")
                resultList.forEach {
                    if (result.textBlocks.find { it.text.contains(targetContent) } != null) {
                        resultValue =
                            Pair(result.textBlocks.find { it.text.contains(targetContent) }, true)
                    }
                }
            }
        }
        return resultValue
    }
    private fun saveToast(autoInfo: AutoInfo, toast: String) {
        FloatingWindowService.toastTip?.value = ""
    }
}*/
