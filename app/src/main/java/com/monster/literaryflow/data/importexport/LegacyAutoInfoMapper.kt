package com.monster.literaryflow.data.importexport

import com.monster.literaryflow.bean.AutoInfo
import com.monster.literaryflow.bean.AutoRunType
import com.monster.literaryflow.bean.ClickBean
import com.monster.literaryflow.bean.RuleType
import com.monster.literaryflow.bean.RunBean
import com.monster.literaryflow.bean.RunType
import com.monster.literaryflow.bean.TextPickType
import com.monster.literaryflow.bean.TriggerBean
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.ConditionSpec
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.StepType
import com.monster.literaryflow.core.model.SwipeSpec
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.model.AppProfileModel
import kotlin.math.abs

object LegacyAutoInfoMapper {
    private const val LEGACY_BASE_WIDTH = 1080f
    private const val LEGACY_BASE_HEIGHT = 1920f

    fun convertAll(legacyTasks: List<AutoInfo>): AutomationDocument {
        val now = System.currentTimeMillis()
        val tasks = legacyTasks.mapIndexed { index, legacy ->
            convertTask(legacy, index, now)
        }
        return AutomationDocument(
            schemaVersion = AutomationDocument.CURRENT_SCHEMA_VERSION,
            exportedAt = now,
            tasks = tasks,
            appProfiles = tasks.mapNotNull { task ->
                task.targetPackageName.takeIf { it.isNotBlank() }?.let { packageName ->
                    AppProfileModel(
                        id = packageName.hashCode().toLong().let { if (it == 0L) now else it },
                        packageName = packageName,
                        appName = task.targetAppName,
                        defaultRecognitionSource = RecognitionSource.AUTO
                    )
                }
            }.distinctBy { it.packageName }
        )
    }

    fun convertTask(legacy: AutoInfo, index: Int = 0, now: Long = System.currentTimeMillis()): TaskModel {
        val taskId = stableTaskId(legacy, index)
        val steps = legacy.runInfo.orEmpty().mapIndexed { stepIndex, runBean ->
            convertRunBean(
                taskId = taskId,
                orderIndex = stepIndex,
                runBean = runBean
            )
        }

        return TaskModel(
            id = taskId,
            title = legacy.title?.takeIf { it.isNotBlank() } ?: "旧版任务 ${index + 1}",
            enabled = legacy.isRun,
            targetPackageName = legacy.runPackageName.orEmpty(),
            targetAppName = legacy.runAppName ?: legacy.runPackageName.orEmpty(),
            allowAutoOpenApp = legacy.runState,
            scheduleType = legacy.loopType.toScheduleType(),
            startTime = legacy.runTime?.first?.toClockText(),
            endTime = legacy.runTime?.second?.toClockText(),
            weeklyDays = legacy.weekData.takeIf { it.isNotEmpty() }?.joinToString(","),
            runTimes = legacy.runTimes.coerceAtLeast(1),
            intervalMs = (legacy.sleepTime.coerceAtLeast(0) * 1000L).takeIf { it > 0L } ?: 0L,
            createdAt = now,
            updatedAt = now,
            schemaVersion = AutomationDocument.CURRENT_SCHEMA_VERSION,
            steps = steps
        )
    }

    private fun convertRunBean(
        taskId: Long,
        orderIndex: Int,
        runBean: RunBean
    ): StepModel {
        val stepId = stableStepId(taskId, orderIndex)
        val clickBean = runBean.clickBean
        return if (clickBean != null) {
            StepModel(
                id = stepId,
                taskId = taskId,
                orderIndex = orderIndex,
                type = clickBean.clickType.toStepType(),
                title = clickBean.clickType.toDisplayName(),
                timeoutMs = clickBean.timeoutMs(),
                delayAfterMs = clickBean.sleepTime.secondsToMs(),
                failurePolicy = FailurePolicy.STOP,
                actions = listOf(convertClickBean(clickBean))
            )
        } else {
            StepModel(
                id = stepId,
                taskId = taskId,
                orderIndex = orderIndex,
                type = StepType.CONDITION,
                title = "条件判断",
                timeoutMs = runBean.triggerBean.maxOfOrNull { it.runScanTime.secondsToMs() } ?: 3000L,
                delayAfterMs = 0L,
                failurePolicy = FailurePolicy.STOP,
                conditions = runBean.triggerBean.map { convertTriggerBean(it) }
            )
        }
    }

    private fun convertTriggerBean(trigger: TriggerBean): ConditionSpec {
        return when (trigger.triggerType) {
            RuleType.TIME -> ConditionSpec(
                type = ConditionType.TIME_WINDOW,
                startTime = trigger.runTime?.first?.toClockText(),
                endTime = trigger.runTime?.second?.toClockText(),
                timeoutMs = trigger.runScanTime.secondsToMs(),
                onTrueActions = trigger.runTrueTask?.let { listOf(convertClickBean(it)) }.orEmpty() +
                    trigger.runTrueAuto?.second.orEmpty().flatMap { convertNestedRunBeanActions(it) },
                onFalseActions = trigger.runFalseTask?.let { listOf(convertClickBean(it)) }.orEmpty() +
                    trigger.runFalseAuto?.second.orEmpty().flatMap { convertNestedRunBeanActions(it) },
                legacyNote = "由旧版 TIME 触发器转换"
            )
            RuleType.FIND_TEXT, null -> ConditionSpec(
                type = ConditionType.TEXT_VISIBLE,
                text = trigger.findText,
                matchType = trigger.findTextType.toMatchType(),
                recognitionSource = trigger.isFindText4Node.toRecognitionSource(),
                timeoutMs = trigger.runScanTime.secondsToMs(),
                onTrueActions = trigger.runTrueTask?.let { listOf(convertClickBean(it)) }.orEmpty() +
                    trigger.runTrueAuto?.second.orEmpty().flatMap { convertNestedRunBeanActions(it) },
                onFalseActions = trigger.runFalseTask?.let { listOf(convertClickBean(it)) }.orEmpty() +
                    trigger.runFalseAuto?.second.orEmpty().flatMap { convertNestedRunBeanActions(it) },
                legacyNote = "由旧版 FIND_TEXT 触发器转换"
            )
        }
    }

    private fun convertNestedRunBeanActions(runBean: RunBean): List<ActionSpec> {
        runBean.clickBean?.let { return listOf(convertClickBean(it)) }
        return runBean.triggerBean.map {
            ActionSpec(
                type = ActionType.WAIT_FOR_TEXT,
                text = it.findText,
                matchType = it.findTextType.toMatchType(),
                recognitionSource = it.isFindText4Node.toRecognitionSource(),
                timeoutMs = it.runScanTime.secondsToMs(),
                legacyNote = "旧版嵌套触发器已折叠为等待动作"
            )
        }
    }

    private fun convertClickBean(clickBean: ClickBean): ActionSpec {
        return when (clickBean.clickType) {
            RunType.CLICK_XY -> ActionSpec(
                type = ActionType.TAP_COORDINATE,
                x = clickBean.clickXy.first.toNormalizedX(),
                y = clickBean.clickXy.second.toNormalizedY(),
                timeoutMs = clickBean.timeoutMs(),
                repeatCount = clickBean.loopTimes.coerceAtLeast(1),
                legacyNote = "旧版绝对坐标按 1080x1920 基准归一化，导入后建议重新校准"
            )
            RunType.CLICK_TEXT -> ActionSpec(
                type = ActionType.TAP_TEXT,
                text = clickBean.text,
                matchType = clickBean.findTextType.toMatchType(),
                recognitionSource = clickBean.isFindText4Node.toRecognitionSource(),
                timeoutMs = clickBean.timeoutMs(),
                repeatCount = clickBean.loopTimes.coerceAtLeast(1)
            )
            RunType.SCROLL_LEFT,
            RunType.SCROLL_RIGHT,
            RunType.SCROLL_TOP,
            RunType.SCROLL_BOTTOM,
            RunType.LONG_HOR,
            RunType.LONG_VEH -> ActionSpec(
                type = ActionType.SWIPE,
                swipe = clickBean.clickType.toSwipeSpec(clickBean.scrollTime),
                timeoutMs = clickBean.timeoutMs(),
                repeatCount = clickBean.loopTimes.coerceAtLeast(1)
            )
            RunType.ENTER_TEXT -> ActionSpec(
                type = ActionType.INPUT_TEXT,
                inputText = clickBean.enterText.orEmpty(),
                timeoutMs = clickBean.timeoutMs()
            )
            RunType.GO_BACK -> ActionSpec(
                type = ActionType.BACK,
                timeoutMs = clickBean.timeoutMs()
            )
            RunType.OPEN_APP -> ActionSpec(
                type = ActionType.OPEN_APP,
                targetAppName = clickBean.openAppData?.first,
                targetPackageName = clickBean.openAppData?.second,
                timeoutMs = clickBean.timeoutMs()
            )
            RunType.TASK -> ActionSpec(
                type = ActionType.RUN_SUBTASK,
                childTaskTitle = clickBean.runTask?.first,
                timeoutMs = clickBean.timeoutMs(),
                legacyNote = "旧版子任务只有标题和内嵌步骤，导入后需要重新绑定为新版任务"
            )
            RunType.LONG_CLICK -> ActionSpec(
                type = ActionType.LONG_PRESS,
                x = clickBean.clickXy.first.toNormalizedX(),
                y = clickBean.clickXy.second.toNormalizedY(),
                timeoutMs = clickBean.longClickTime.secondsToMs(),
                repeatCount = clickBean.loopTimes.coerceAtLeast(1),
                legacyNote = "旧版长按坐标按 1080x1920 基准归一化"
            )
            null -> ActionSpec(
                type = ActionType.WAIT,
                timeoutMs = clickBean.timeoutMs(),
                legacyNote = "旧版动作类型为空，已转换为等待"
            )
        }
    }

    private fun stableTaskId(legacy: AutoInfo, index: Int): Long {
        return if (legacy.id > 0) legacy.id.toLong() else -(index + 1L)
    }

    private fun stableStepId(taskId: Long, orderIndex: Int): Long {
        val seed = abs(taskId).coerceAtLeast(1L)
        return seed * 10_000L + orderIndex + 1L
    }

    private fun AutoRunType.toScheduleType(): ScheduleType = when (this) {
        AutoRunType.DAY_LOOP -> ScheduleType.DAILY
        AutoRunType.WEEK_LOOP -> ScheduleType.WEEKLY
        AutoRunType.LOOP,
        AutoRunType.RUNNING -> ScheduleType.LOOP
        AutoRunType.SPECIFIED_NUMBER -> ScheduleType.RUN_COUNT
        AutoRunType.OVER -> ScheduleType.MANUAL
    }

    private fun RunType?.toStepType(): StepType = when (this) {
        RunType.TASK -> StepType.SUBTASK
        null -> StepType.WAIT
        else -> StepType.ACTION
    }

    private fun RunType?.toDisplayName(): String = when (this) {
        RunType.CLICK_XY -> "点击坐标"
        RunType.CLICK_TEXT -> "点击文字"
        RunType.SCROLL_LEFT -> "左滑"
        RunType.SCROLL_RIGHT -> "右滑"
        RunType.SCROLL_TOP -> "上滑"
        RunType.SCROLL_BOTTOM -> "下滑"
        RunType.ENTER_TEXT -> "输入文字"
        RunType.GO_BACK -> "返回"
        RunType.TASK -> "运行子任务"
        RunType.OPEN_APP -> "打开应用"
        RunType.LONG_CLICK -> "长按"
        RunType.LONG_HOR -> "横向长滑"
        RunType.LONG_VEH -> "纵向长滑"
        null -> "等待"
    }

    private fun TextPickType.toMatchType(): MatchType = when (this) {
        TextPickType.EXACT_MATCH -> MatchType.EXACT
        TextPickType.FUZZY_MATCH -> MatchType.FUZZY
        TextPickType.MULTIPLE_FUZZY_WORDS -> MatchType.ANY_KEYWORD
    }

    private fun Boolean.toRecognitionSource(): RecognitionSource {
        return if (this) RecognitionSource.ACCESSIBILITY else RecognitionSource.OCR
    }

    private fun Pair<Int, Int>.toClockText(): String {
        return "%02d:%02d".format(first.coerceIn(0, 23), second.coerceIn(0, 59))
    }

    private fun Int.secondsToMs(): Long = coerceAtLeast(0) * 1000L

    private fun ClickBean.timeoutMs(): Long {
        return findTextTime.coerceAtLeast(1) * 1000L
    }

    private fun Int.toNormalizedX(): Float = (this / LEGACY_BASE_WIDTH).coerceIn(0f, 1f)

    private fun Int.toNormalizedY(): Float = (this / LEGACY_BASE_HEIGHT).coerceIn(0f, 1f)

    private fun RunType?.toSwipeSpec(scrollTime: Int): SwipeSpec {
        val duration = scrollTime.coerceAtLeast(1) * 100L
        return when (this) {
            RunType.SCROLL_LEFT,
            RunType.LONG_HOR -> SwipeSpec(0.78f, 0.5f, 0.22f, 0.5f, duration)
            RunType.SCROLL_RIGHT -> SwipeSpec(0.22f, 0.5f, 0.78f, 0.5f, duration)
            RunType.SCROLL_TOP,
            RunType.LONG_VEH -> SwipeSpec(0.5f, 0.75f, 0.5f, 0.25f, duration)
            RunType.SCROLL_BOTTOM -> SwipeSpec(0.5f, 0.25f, 0.5f, 0.75f, duration)
            else -> SwipeSpec(0.5f, 0.75f, 0.5f, 0.25f, duration)
        }
    }
}
