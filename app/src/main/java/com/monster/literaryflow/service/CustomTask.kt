import android.util.Log
import cn.coderpig.cp_fast_accessibility.NodeWrapper
import cn.coderpig.cp_fast_accessibility.back
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.startSliding
import cn.coderpig.cp_fast_accessibility.swipe
import com.hjq.permissions.PermissionFragment.launch
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.MyAccessibilityService
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.autoRun.AutoRunManager
import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.room.AutoInfoDao
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.PriorityQueue

sealed class TaskState {
    object Idle : TaskState()
    object Running : TaskState()
    object Paused : TaskState()
    object Cancelled : TaskState()
}

class CustomTask(private val autoInfoDao: AutoInfoDao) {
    private val taskChannel = Channel<List<AutoInfo>>(Channel.UNLIMITED) // 用于传递任务数据
    private var taskJob: Job? = null
    private val _state = MutableStateFlow<TaskState>(TaskState.Idle)
    val state = _state.asStateFlow()
    // 新增优先级任务队列
    private val activeMonitors = mutableMapOf<String, Job>()
    private val priorityQueue = PriorityQueue<AutoInfo>(compareByDescending { it.priority })
    private var currentJob: Job? = null
    private val preemptionLock = Mutex()

    private var accessibilityService: MyAccessibilityService? = null
    private var imgJob: Job? = null
    private val _imgState = MutableStateFlow<TaskState>(TaskState.Idle)

    // 启动任务
    fun start() {
        if (_state.value == TaskState.Running) return
        _state.value = TaskState.Running

        // 启动优先级监听协程
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                priorityQueue.poll()?.let { autoInfo ->
                    executeWithInterruptible(autoInfo).also {
                        if (it) priorityQueue.add(autoInfo)
                    }
                }
                delay(100)
            }
        }
    }
    // 修改后的执行方法
    private suspend fun executeWithInterruptible(autoInfo: AutoInfo): Boolean {
        return coroutineScope {
            preemptionLock.lock()
            val job = launch(Dispatchers.IO) {
                runLoop(autoInfo.runTimes - autoInfo.todayRunTime.second, autoInfo)
            }

            currentJob = job
            preemptionLock.unlock()

            job.join()
            currentJob = null

            !job.isCancelled && checkStillHighest(autoInfo)
        }
    }
    // 新增优先级校验方法
    private fun checkStillHighest(current: AutoInfo): Boolean {
        return priorityQueue.none {
            it.priority > current.priority && it.isRun
        }
    }

    // 取消任务
    fun cancel() {
        CoroutineScope(Dispatchers.IO).launch {
            // 阶段1：执行取消任务链
            currentJob?.let {
                activeMonitors.values.forEach { it.cancel() }
                priorityQueue.peek()?.cancelTasks?.forEach { runBean ->
                    runAuto(runBean)
                }
            }

            // 阶段2：终止任务执行
            _state.value = TaskState.Cancelled
            currentJob?.cancel()
            taskJob?.cancel()

            // 阶段3：清理资源
            withContext(Dispatchers.Main) {
                accessibilityService?.disableSelf()
                accessibilityService = null

            }
            priorityQueue.clear()
            activeMonitors.clear()
        }
    }

    // 新增任务提交方法
    suspend fun submitTask(autoInfo: AutoInfo) {
        if (!priorityQueue.contains(autoInfo)) {
            priorityQueue.poll()?.let { autoInfo ->
                if (!executeWithInterruptible(autoInfo)) {
                    priorityQueue.add(autoInfo)  // 任务未完成时重新入队
                }
            }
        }
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
                if (autoInfo.runTimes > autoInfo.todayRunTime.second) {
                    Log.d(
                        "FloatService",
                        "${autoInfo.title} times: ${autoInfo.runTimes},alreadyRunTimes:${autoInfo.todayRunTime.second}"
                    )
                    runLoop(autoInfo.runTimes - autoInfo.todayRunTime.second, autoInfo)
                } else {
                    Log.d("FloatService", "${autoInfo.title} run over")
                }

            }
        }
    }

    private suspend fun runLoop(times: Int, autoInfo: AutoInfo) {
        var index = 0
        while (times == -1 || index < times) {
            if (!checkHigherPriority(autoInfo)) { // 检查是否有更高优先级任务
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
                    if (autoInfo.runInfo != null) {
                        for (runBean in autoInfo.runInfo!!) {
                            runAuto(runBean)
                        }
                    }
                } else {
                    return
                }
                val runTimes = autoInfo.todayRunTime.second + 1
                autoInfo.todayRunTime = Pair(System.currentTimeMillis(), runTimes)
                autoInfoDao.update(autoInfo)
                index++
            }else {
                Log.d("FloatService", "有更高优先级任务，跳过")
            }
        }
    }
    private fun checkHigherPriority(current: AutoInfo): Boolean {
        return priorityQueue.any { it.priority > current.priority && it.isRun }
    }
    private suspend fun runAuto(runBean: RunBean) {
        if (runBean.clickBean != null) {
            runTask(runBean.clickBean!!)
        } else if (runBean.triggerBean != null) {
            runTrigger(runBean.triggerBean!!)
        }
    }

    private suspend fun runTrigger(triggerBean: TriggerBean) {
        var isLoop = true
        val endTime = System.currentTimeMillis() + triggerBean.runScanTime * 1000L
        var nodeList: ArrayList<NodeWrapper>? = arrayListOf()
        while (System.currentTimeMillis() < endTime && isLoop) {
            if (System.currentTimeMillis() >= endTime) {
                isLoop = false
            }
            when (triggerBean.triggerType) {
                RuleType.TIME -> {
                    if (TimeUtils.isInCurrentTime(triggerBean.runTime!!)) {
                        if (triggerBean.runTrueTask != null) {
                            runTask(triggerBean.runTrueTask!!)
                        } else if (triggerBean.runTrueAuto != null) {
                            for (runBean in triggerBean.runTrueAuto!!.second!!) {
                                runAuto(runBean)
                            }
                        }
                        break
                    }
                }

                RuleType.FIND_TEXT -> {
                    if (triggerBean.isFindText4Node) {
                        if (accessibilityService != null) {
                            // 检查是否存在文字
                            val textFound = accessibilityService!!.findText(triggerBean.findText!!)
                            Log.d("AnotherService", "find4Node Text found: $textFound")
                            if (textFound.first) {
                                if (triggerBean.runTrueTask != null) {
                                    runTask(triggerBean.runTrueTask!!)
                                } else if (triggerBean.runTrueAuto != null) {
                                    for (runBean in triggerBean.runTrueAuto!!.second!!) {
                                        runAuto(runBean)
                                    }
                                }
                                break
                            } else {
                                if (triggerBean.runFalseTask != null) {
                                    runTask(triggerBean.runFalseTask!!)
                                } else if (triggerBean.runFalseAuto != null) {
                                    for (runBean in triggerBean.runFalseAuto!!.second!!) {
                                        runAuto(runBean)
                                    }
                                }
                            }
                        } else {
                            Log.e("AnotherService", "MyAccessibilityService is not active")
                        }
                    } else {
                        Log.d("AnotherService", "find4OCR Text found: ${triggerBean.findText}")

                        val nodeResult = triggerBean.findText?.let { it1 ->
                            AutoRunManager.findText(
                                it1,
                                triggerBean.findTextType,
                                triggerBean.runScanTime
                            )
                        }
                        if (nodeResult?.first == true) {
                            if (triggerBean.runTrueTask != null) {
                                runTask(triggerBean.runTrueTask!!)
                            } else if (triggerBean.runTrueAuto != null) {
                                for (runBean in triggerBean.runTrueAuto!!.second!!) {
                                    runAuto(runBean)
                                }
                            }
                        } else {
                            if (triggerBean.runFalseTask != null) {
                                runTask(triggerBean.runFalseTask!!)
                            } else if (triggerBean.runFalseAuto != null) {
                                for (runBean in triggerBean.runFalseAuto!!.second!!) {
                                    runAuto(runBean)
                                }
                            }
                        }
                    }

                }

                else -> {}
            }

        }
        if (!isLoop) {
            if (triggerBean.runFalseTask != null) {
                runTask(triggerBean.runFalseTask!!)
            } else if (triggerBean.runFalseAuto != null) {
                for (runBean in triggerBean.runFalseAuto!!.second!!) {
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
                    if (clickBean.isFindText4Node) {
                        if (accessibilityService != null) {
                            clickBean.findTextType
                            // 点击文字
                            val clickNode =
                                clickBean.text?.let { it1 ->
                                    accessibilityService!!.clickText(
                                        it1,
                                        clickBean.findTextType
                                    )
                                }
                            Log.d("AnotherService", "Text clicked: 【${clickBean.text}】$clickNode")
                        } else {
                            Log.e("AnotherService", "MyAccessibilityService is not active")
                        }
                    } else {
                        val nodeResult = clickBean.text?.let { it1 ->
                            AutoRunManager.findText(
                                it1,
                                clickBean.findTextType,
                                clickBean.findTextTime
                            )
                        }
                        if (nodeResult?.first == true) {
                            val rect = nodeResult.second!!.boundingBox!!
                            click(rect.centerX(), rect.centerY())
                        }
                    }
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

                RunType.TASK -> {
                    Log.d("FloatService", "执行任务【${clickBean.runTask?.first}】}")
                    for (runBean in clickBean.runTask?.second!!) {
                        runAuto(runBean)
                    }
                }

                RunType.GO_BACK -> {
                    Log.d("FloatService", "侧滑返回")
                    back()
                }

                RunType.OPEN_APP -> {
                    clickBean.openAppData?.second?.let { it1 ->
                        AppUtils.openApp(
                            MyApp.instance,
                            it1
                        )
                    }
                }

                RunType.LONG_CLICK -> {
                    click(
                        clickBean!!.clickXy.first,
                        clickBean.clickXy.second,
                        duration = clickBean.longClickTime * 1000L
                    )
                    Log.d(
                        "FloatService",
                        "长按:${clickBean.clickXy.first}，${clickBean.clickXy.second} ,${clickBean.longClickTime}秒"
                    )
                }
            }
            delay(clickBean.sleepTime * 1000L)
            Log.d("FloatService", "等待${clickBean.sleepTime}秒")
        }
    }

}