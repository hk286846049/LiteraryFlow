package com.monster.literaryflow.autoRun.task

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// 任务类型定义
sealed class TaskSchedule {
    data class Daily(val startTime: Pair<Int,Int>, val endTime:  Pair<Int,Int>) : TaskSchedule()
    data class Weekly(val dayOfWeek: MutableList<Int>, val startTime: Pair<Int,Int>, val endTime: Pair<Int,Int>) : TaskSchedule()
    data class Interval(val delayMillis: Long) : TaskSchedule()
    data class DailyAt(val totalDegree: Int, var runDegree: Int = 0) : TaskSchedule()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDateTime.isInTimeRange(start: LocalTime, end: LocalTime): Boolean {
    val localTime = this.toLocalTime()
    return when {
        start <= end -> localTime in start..end
        else -> localTime >= start || localTime <= end // 处理跨天
    }
}

// 任务状态
enum class TaskState {
    RUNNING, PAUSED, CANCELLED
}

// 增强版任务定义
data class ScheduledTask(
    val name: String,
    val schedule: TaskSchedule,
    var state: TaskState = TaskState.RUNNING,
    val action: suspend () -> Unit,
)

// 增强版任务调度器
@RequiresApi(Build.VERSION_CODES.O)
class TaskScheduler {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tasks = mutableListOf<ScheduledTask>()
    private val executionMutex = Mutex()
    private val intervalJobs = mutableMapOf<String, Job?>()
    private val activeIntervalTasks = mutableSetOf<String>()
    private val activeIntervalMutex = Mutex()
    private val pauseRequests = mutableMapOf<String, CompletableDeferred<Unit>>()
    private var dailyResetJob: Job? = null
    private val dailyAtTasks = mutableMapOf<String, Pair<LocalDate, Int>>()

/*
    fun init() {
        Log.i("任务调度器", "====== 初始化任务调度器 ======")
        startDailyResetChecker()
    }
*/

    @RequiresApi(Build.VERSION_CODES.O)
    fun startDailyResetChecker() {
        Log.d("每日重置检查", "启动每日重置检查任务")
        dailyResetJob = scope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val delayMillis = Duration.between(now, nextMidnight).toMillis()

                Log.d("每日重置检查", "等待到次日零点，剩余时间: ${delayMillis/1000}秒")
                delay(delayMillis)

                // 重置所有DailyAt任务的执行次数
                tasks.filter { it.schedule is TaskSchedule.DailyAt }.forEach { task ->
                    (task.schedule as TaskSchedule.DailyAt).runDegree = 0
                    Log.d("每日重置", "重置任务 ${task.name} 的执行次数")
                }
                dailyAtTasks.clear()
                Log.i("每日重置检查", "已完成所有DailyAt任务重置")
            }
        }
    }

    // 添加任务
    fun addTask(task: ScheduledTask) {
        if (tasks.any { it.name == task.name }) {
            Log.w("任务添加", "任务 ${task.name} 已存在，移除旧任务")
            tasks.removeIf { it.name == task.name }
        }
        tasks.add(task)
        Log.i("任务添加", "成功添加任务: ${task.name} | 类型: ${task.schedule.javaClass.simpleName}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scheduleTask(task)
        }
    }

    // 移除任务
    fun removeTask(taskName: String) {
        Log.d("任务移除", "尝试移除任务: $taskName")
        tasks.removeIf { it.name == taskName }
        intervalJobs[taskName]?.cancel()
        intervalJobs.remove(taskName)
        pauseRequests.remove(taskName)
        dailyAtTasks.remove(taskName)
        Log.i("任务移除", "成功移除任务: $taskName")
    }

    // 暂停任务
    suspend fun pauseTask(taskName: String) {
        Log.d("任务暂停", "尝试暂停任务: $taskName")
        val task = tasks.find { it.name == taskName } ?: run {
            Log.w("任务暂停", "任务 $taskName 不存在")
            return
        }
        task.state = TaskState.PAUSED

        if (task.schedule is TaskSchedule.Interval || task.schedule is TaskSchedule.DailyAt) {
            intervalJobs[taskName]?.cancel()
            intervalJobs.remove(taskName)
            Log.d("任务暂停", "已暂停循环任务: $taskName")
        } else {
            pauseRequests[taskName] = CompletableDeferred()
            Log.d("任务暂停", "已设置定时任务暂停标记: $taskName")
        }
    }

    // 继续任务
    fun resumeTask(taskName: String) {
        Log.d("任务恢复", "尝试恢复任务: $taskName")
        val task = tasks.find { it.name == taskName } ?: run {
            Log.w("任务恢复", "任务 $taskName 不存在")
            return
        }
        task.state = TaskState.RUNNING

        if (task.schedule is TaskSchedule.Interval || task.schedule is TaskSchedule.DailyAt) {
            if (intervalJobs[taskName]?.isActive != true) {
                intervalJobs[taskName] = when (task.schedule) {
                    is TaskSchedule.Interval -> scheduleIntervalTask(task)
                    is TaskSchedule.DailyAt -> scheduleDailyAtTask(task)
                    else -> null
                }
                Log.d("任务恢复", "已重新调度循环任务: $taskName")
            }
        } else {
            pauseRequests[taskName]?.complete(Unit)
            pauseRequests.remove(taskName)
            Log.d("任务恢复", "已恢复定时任务: $taskName")
        }
    }

    // 暂停所有任务
    suspend fun pauseAll() {
        Log.i("任务管理", "====== 暂停所有任务 ======")
        tasks.forEach { task ->
            pauseTask(task.name)
        }
    }

    // 继续所有任务
    fun resumeAll() {
        Log.i("任务管理", "====== 恢复所有任务 ======")
        tasks.forEach { task ->
            resumeTask(task.name)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNextExecutionTime(schedule: TaskSchedule): Long {
        val now = LocalDateTime.now()
        return when (schedule) {
            is TaskSchedule.Daily -> {
                val start = LocalTime.of(schedule.startTime.first, schedule.startTime.second)
                val end = LocalTime.of(schedule.endTime.first, schedule.endTime.second)

                val baseDate = now.toLocalDate()
                val startToday = baseDate.atTime(start)
                val endToday = if (start < end) baseDate.atTime(end) else baseDate.plusDays(1).atTime(end)

                val nextTime = when {
                    now.isInTimeRange(start, end) -> now
                    now < startToday -> startToday
                    else -> startToday.plusDays(1)
                }
                Log.d("时间计算", "每日任务下次执行时间: $nextTime")
                nextTime
            }

            is TaskSchedule.Weekly -> {
                val start = LocalTime.of(schedule.startTime.first, schedule.startTime.second)
                val end = LocalTime.of(schedule.endTime.first, schedule.endTime.second)

                val nextTime = schedule.dayOfWeek
                    .mapNotNull { day ->
                        val dayOfWeek = DayOfWeek.of(day)
                        val date = now.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                            .toLocalDate()

                        val startDateTime = date.atTime(start)
                        val endDateTime = if (start < end) {
                            date.atTime(end)
                        } else {
                            date.plusDays(1).atTime(end)
                        }

                        when {
                            now.isBefore(startDateTime) -> startDateTime
                            now.isBefore(endDateTime) -> now
                            else -> startDateTime.plusWeeks(1)
                        }
                    }
                    .minOrNull() ?: throw IllegalStateException("No valid weekly schedule")
                Log.d("时间计算", "每周任务下次执行时间: $nextTime")
                nextTime
            }

            is TaskSchedule.Interval -> {
                val nextTime = now.plus(schedule.delayMillis, ChronoUnit.MILLIS)
                Log.d("时间计算", "间隔任务下次执行时间: $nextTime (间隔: ${schedule.delayMillis}ms)")
                nextTime
            }
            is TaskSchedule.DailyAt -> {
                Log.d("时间计算", "DailyAt任务不需要计算下次执行时间")
                now
            }
        }.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleTask(task: ScheduledTask) {
        scope.launch {
            Log.i("任务调度", "开始调度任务: ${task.name} | 类型: ${task.schedule.javaClass.simpleName}")
            when (task.schedule) {
                is TaskSchedule.Daily, is TaskSchedule.Weekly -> {
                    while (isActive && task.state != TaskState.CANCELLED) {
                        if (task.state == TaskState.PAUSED) {
                            Log.d("任务等待", "任务 ${task.name} 处于暂停状态，等待恢复")
                            pauseRequests[task.name]?.await()
                            continue
                        }

                        val nextExecutionTime = getNextExecutionTime(task.schedule)
                        val delayMillis = nextExecutionTime - System.currentTimeMillis()

                        if (delayMillis > 0) {
                            Log.d("任务等待", "任务 ${task.name} 等待 ${delayMillis/1000}秒后执行")
                            delay(delayMillis)
                        }

                        executionMutex.withLock {
                            val current = LocalDateTime.now()
                            val isValid = when (val s = task.schedule) {
                                is TaskSchedule.Daily -> current.isInTimeRange(
                                    LocalTime.of(s.startTime.first, s.startTime.second),
                                    LocalTime.of(s.endTime.first, s.endTime.second)
                                )
                                is TaskSchedule.Weekly -> current.isInTimeRange(
                                    LocalTime.of(s.startTime.first, s.startTime.second),
                                    LocalTime.of(s.endTime.first, s.endTime.second)
                                ) && s.dayOfWeek.contains(current.dayOfWeek.value)
                                else -> false
                            }

                            if (isValid) {
                                Log.i("任务执行", "====== 开始执行任务 ${task.name} ======")
                                suspendAllIntervalTasks()
                                try {
                                    task.action()
                                } finally {
                                    resumeAllIntervalTasks()
                                }
                                Log.i("任务执行", "====== 完成任务 ${task.name} ======")
                            } else {
                                Log.d("任务检查", "当前时间不在任务 ${task.name} 的有效执行时段")
                            }
                        }

                        // 防止连续执行
                        delay(1000)
                    }
                }
                is TaskSchedule.Interval -> {
                    if (task.state == TaskState.RUNNING) {
                        intervalJobs[task.name] = scheduleIntervalTask(task)
                    }
                }
                is TaskSchedule.DailyAt -> {
                    if (task.state == TaskState.RUNNING) {
                        intervalJobs[task.name] = scheduleDailyAtTask(task)
                    }
                }
            }
            Log.i("任务结束", "任务 ${task.name} 调度结束")
        }
    }

    private fun scheduleDailyAtTask(task: ScheduledTask): Job {
        Log.i("任务调度", "开始调度DailyAt任务: ${task.name}")
        val dailyAtSchedule = task.schedule as TaskSchedule.DailyAt
        return scope.launch {
            while (isActive && task.state != TaskState.CANCELLED) {
                if (task.state == TaskState.PAUSED) {
                    Log.d("任务等待", "DailyAt任务 ${task.name} 处于暂停状态，等待恢复")
                    pauseRequests[task.name]?.await()
                    continue
                }

                val today = LocalDate.now()
                val lastExecutedDate = dailyAtTasks[task.name]?.first

                // 如果日期变化，重置执行次数
                if (lastExecutedDate != today) {
                    dailyAtSchedule.runDegree = 0
                    dailyAtTasks[task.name] = today to 0
                    Log.d("每日重置", "重置DailyAt任务 ${task.name} 的执行次数")
                }

                if (dailyAtSchedule.runDegree < dailyAtSchedule.totalDegree) {
                    executionMutex.withLock {
                        if (dailyAtSchedule.runDegree < dailyAtSchedule.totalDegree) {
                            try {
                                activeIntervalMutex.withLock {
                                    activeIntervalTasks.add(task.name)
                                }
                                Log.i("任务执行", "====== 开始执行DailyAt任务 ${task.name} (${dailyAtSchedule.runDegree + 1}/${dailyAtSchedule.totalDegree}) ======")
                                task.action()
                                dailyAtSchedule.runDegree++
                                dailyAtTasks[task.name] = today to dailyAtSchedule.runDegree
                                Log.i("任务执行", "====== 完成DailyAt任务 ${task.name} (${dailyAtSchedule.runDegree}/${dailyAtSchedule.totalDegree}) ======")
                            } finally {
                                activeIntervalMutex.withLock {
                                    activeIntervalTasks.remove(task.name)
                                }
                            }
                        }
                    }
                }

                // 检查是否已完成今日所有执行次数
                if (dailyAtSchedule.runDegree >= dailyAtSchedule.totalDegree) {
                    val now = LocalDateTime.now()
                    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                    val delayMillis = Duration.between(now, nextMidnight).toMillis()
                    Log.d("任务等待", "DailyAt任务 ${task.name} 今日已完成，等待次日零点 (${delayMillis/1000}秒后)")
                    delay(delayMillis)
                } else {
                    Log.d("任务等待", "DailyAt任务 ${task.name} 等待下次执行检查 (1秒后)")
                    delay(1000)
                }
            }
            Log.i("任务结束", "DailyAt任务 ${task.name} 调度结束")
        }
    }

    private suspend fun suspendAllIntervalTasks() {
        Log.i("任务管理", "====== 暂停所有循环任务 ======")
        intervalJobs.values.forEach { job ->
            job?.cancel()
        }
        // 等待所有循环任务完成
        while (true) {
            val activeCount = activeIntervalMutex.withLock { activeIntervalTasks.size }
            if (activeCount == 0) break
            Log.d("任务等待", "等待 ${activeCount} 个循环任务完成...")
            delay(100)
        }
        intervalJobs.clear()
        Log.i("任务管理", "所有循环任务已暂停")
    }

    private fun resumeAllIntervalTasks() {
        Log.i("任务管理", "====== 恢复所有循环任务 ======")
        tasks.filter {
            (it.schedule is TaskSchedule.Interval || it.schedule is TaskSchedule.DailyAt) &&
                    it.state == TaskState.RUNNING
        }.forEach {
            if (intervalJobs[it.name]?.isActive != true) {
                intervalJobs[it.name] = when (it.schedule) {
                    is TaskSchedule.Interval -> scheduleIntervalTask(it)
                    is TaskSchedule.DailyAt -> scheduleDailyAtTask(it)
                    else -> null
                }
                Log.d("任务恢复", "已恢复循环任务: ${it.name}")
            }
        }
    }

    private fun scheduleIntervalTask(task: ScheduledTask): Job {
        Log.i("任务调度", "开始调度间隔任务: ${task.name} | 间隔: ${(task.schedule as TaskSchedule.Interval).delayMillis}ms")
        return scope.launch {
            while (isActive && task.state != TaskState.CANCELLED) {
                if (task.state == TaskState.PAUSED) {
                    Log.d("任务等待", "间隔任务 ${task.name} 处于暂停状态，等待恢复")
                    pauseRequests[task.name]?.await()
                    pauseRequests.remove(task.name)
                    continue
                }

                try {
                    activeIntervalMutex.withLock {
                        activeIntervalTasks.add(task.name)
                    }

                    executionMutex.withLock {
                        Log.i("任务执行", "====== 开始执行间隔任务 ${task.name} ======")
                        task.action()
                        Log.i("任务执行", "====== 完成间隔任务 ${task.name} ======")
                    }
                } finally {
                    activeIntervalMutex.withLock {
                        activeIntervalTasks.remove(task.name)
                    }
                }

                val delayTime = (task.schedule as TaskSchedule.Interval).delayMillis
                Log.d("任务等待", "间隔任务 ${task.name} 等待下次执行 (${delayTime}ms后)")
                delay(delayTime)
            }
            Log.i("任务结束", "间隔任务 ${task.name} 调度结束")
        }
    }

    // 取消所有任务
    fun cancelAll() {
        Log.i("任务管理", "====== 取消所有任务 ======")
        tasks.forEach { it.state = TaskState.CANCELLED }
        scope.cancel()
        dailyResetJob?.cancel()
        Log.i("任务管理", "所有任务已取消")
    }

    fun removeAll() {
        tasks = mutableListOf<ScheduledTask>()
    /*    Log.i("任务管理", "====== 移除所有任务 ======")
        tasks.clear()
        intervalJobs.clear()
        pauseRequests.clear()
        dailyAtTasks.clear()
        scope.cancel()
        dailyResetJob?.cancel()
        Log.i("任务管理", "所有任务已移除")*/
    }
}