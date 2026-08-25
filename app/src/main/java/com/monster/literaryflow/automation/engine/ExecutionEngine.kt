package com.monster.literaryflow.automation.engine

import com.monster.literaryflow.automation.action.ActionExecutionContext
import com.monster.literaryflow.automation.action.ActionExecutor
import com.monster.literaryflow.automation.state.RunState
import com.monster.literaryflow.automation.state.RunStatus
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.ConditionSpec
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.PerceptionMode
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.result.ErrorCode
import com.monster.literaryflow.core.result.FlowError
import com.monster.literaryflow.core.result.FlowResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.flow.first

class ExecutionEngine(
    private val actionExecutor: ActionExecutor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val taskResolver: ((Long?, String?) -> TaskModel?)? = null
) {
    private var runningJob: Job? = null
    private val mutableState = MutableStateFlow(RunState())
    private val pauseRequested = MutableStateFlow(false)
    private val taskStack = ArrayDeque<Long>()

    val state: StateFlow<RunState> = mutableState

    fun start(task: TaskModel, context: ActionExecutionContext) {
        runningJob?.cancel(CancellationException("replaced by a new task"))
        runningJob = null
        runningJob = scope.launch {
            runTask(task, context)
        }
    }

    fun cancel() {
        mutableState.value = mutableState.value.copy(status = RunStatus.CANCELLING)
        runningJob?.cancel(CancellationException("user cancelled"))
        runningJob = null
        mutableState.value = RunState(status = RunStatus.CANCELLED)
    }

    fun pause() {
        if (runningJob?.isActive == true) {
            pauseRequested.value = true
            mutableState.value = mutableState.value.copy(status = RunStatus.PAUSED)
        }
    }

    fun resume() {
        if (runningJob?.isActive == true) {
            pauseRequested.value = false
            mutableState.value = mutableState.value.copy(status = RunStatus.RUNNING)
        }
    }

    private suspend fun runTask(task: TaskModel, baseContext: ActionExecutionContext) {
        mutableState.value = RunState(
            taskId = task.id,
            taskTitle = task.title,
            totalSteps = task.steps.size,
            status = RunStatus.RUNNING,
            perceptionMode = PerceptionMode.NORMAL,
            message = "任务开始"
        )
        pauseRequested.value = false

        try {
            taskStack.clear()
            taskStack.add(task.id)
            if (baseContext.autoOpenTargetApp && task.allowAutoOpenApp && task.targetPackageName.isNotBlank()) {
                val launchResult = actionExecutor.execute(
                    ActionSpec(type = ActionType.OPEN_APP, targetPackageName = task.targetPackageName, targetAppName = task.targetAppName, timeoutMs = 5000L),
                    baseContext
                )
                if (launchResult is FlowResult.Failure) {
                    mutableState.value = mutableState.value.copy(status = RunStatus.FAILED, message = launchResult.error.message)
                    return
                }
                delay(300L)
            }
            task.steps.forEachIndexed { index, step ->
                currentCoroutineContext().ensureActiveCompat()
                awaitResume()
                mutableState.value = mutableState.value.copy(
                    currentStepIndex = index,
                    perceptionMode = if (step.conditions.isNotEmpty()) PerceptionMode.FAST else PerceptionMode.IDLE,
                    message = step.title
                )

                val conditionResult = runConditions(step, baseContext.copy(stepId = step.id))
                if (conditionResult is FlowResult.Failure) {
                    mutableState.value = mutableState.value.copy(
                        status = RunStatus.FAILED,
                        message = conditionResult.error.message
                    )
                    return
                }

                val actionResult = runActions(step.actions, step, baseContext.copy(stepId = step.id))
                if (actionResult is FlowResult.Failure) {
                    mutableState.value = mutableState.value.copy(
                        status = RunStatus.FAILED,
                        message = actionResult.error.message
                    )
                    return
                }
            }

            mutableState.value = mutableState.value.copy(
                status = RunStatus.COMPLETED,
                perceptionMode = PerceptionMode.IDLE,
                currentStepIndex = task.steps.size,
                message = "任务完成"
            )
            taskStack.clear()
        } catch (error: CancellationException) {
            mutableState.value = mutableState.value.copy(
                status = RunStatus.CANCELLED,
                perceptionMode = PerceptionMode.IDLE,
                message = "任务已取消"
            )
        } catch (error: Exception) {
            mutableState.value = mutableState.value.copy(
                status = RunStatus.FAILED,
                perceptionMode = PerceptionMode.IDLE,
                message = FlowError(ErrorCode.ACTION_FAILED, error.message ?: "执行失败", error).message
            )
        }
    }

    private suspend fun awaitResume() {
        pauseRequested.first { paused ->
            if (paused) {
                delay(50L)
                false
            } else {
                true
            }
        }
    }

    private fun kotlin.coroutines.CoroutineContext.ensureActiveCompat() {
        if (!isActive) throw CancellationException("engine cancelled")
    }

    private suspend fun runConditions(
        step: StepModel,
        context: ActionExecutionContext
    ): FlowResult<Unit> {
        step.conditions.forEach { condition ->
            currentCoroutineContext().ensureActiveCompat()
            val result = evaluateCondition(condition, context)
            val branchActions = if (result is FlowResult.Success) {
                condition.onTrueActions
            } else {
                condition.onFalseActions
            }
            if (branchActions.isNotEmpty()) {
                val branchResult = runActions(branchActions, step, context)
                if (branchResult is FlowResult.Failure) return branchResult
            }
            if (result is FlowResult.Failure && branchActions.isEmpty()) {
                if (step.failurePolicy.name == "SKIP") return@forEach
                return result
            }
        }
        return FlowResult.Success(Unit)
    }

    private suspend fun evaluateCondition(
        condition: ConditionSpec,
        context: ActionExecutionContext
    ): FlowResult<Unit> {
        val result = when (condition.type) {
            ConditionType.TEXT_VISIBLE -> actionExecutor.execute(
                ActionSpec(
                    type = ActionType.WAIT_FOR_TEXT,
                    text = condition.text,
                    matchType = condition.matchType,
                    recognitionSource = condition.recognitionSource,
                    timeoutMs = condition.timeoutMs
                ),
                context
            )
            ConditionType.TIME_WINDOW -> {
                if (isNowInWindow(condition.startTime, condition.endTime)) {
                    FlowResult.Success(Unit)
                } else {
                    FlowResult.Failure(
                        FlowError(
                            ErrorCode.ACTION_FAILED,
                            "当前时间不在步骤允许执行区间内"
                        )
                    )
                }
            }
            ConditionType.SCREEN_STATE -> actionExecutor.execute(
                ActionSpec(
                    type = ActionType.WAIT_FOR_SCREEN,
                    text = condition.text,
                    matchType = condition.matchType,
                    recognitionSource = condition.recognitionSource,
                    timeoutMs = condition.timeoutMs
                ),
                context
            )
        }
        if (result is FlowResult.Success && !condition.excludedText.isNullOrBlank()) {
            condition.excludedText.split("#", ",", "、")
                .map(String::trim)
                .filter(String::isNotBlank)
                .forEach { excluded ->
                    val excludedResult = actionExecutor.execute(
                        ActionSpec(
                            type = ActionType.WAIT_FOR_TEXT,
                            text = excluded,
                            matchType = MatchType.FUZZY,
                            recognitionSource = condition.recognitionSource,
                            timeoutMs = 250L
                        ),
                        context
                    )
                    if (excludedResult is FlowResult.Success) {
                        return FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "页面包含排除词：$excluded"))
                    }
                }
        }
        return result
    }

    private suspend fun runActions(
        actions: List<ActionSpec>,
        step: StepModel,
        context: ActionExecutionContext
    ): FlowResult<Unit> {
        actions.forEach { action ->
            currentCoroutineContext().ensureActiveCompat()
            awaitResume()
            val result = executeWithPolicy(action, step, context)
            if (result is FlowResult.Failure) return result
            delay(step.delayAfterMs.coerceAtLeast(0L))
        }
        return FlowResult.Success(Unit)
    }

    private suspend fun executeWithPolicy(
        action: ActionSpec,
        step: StepModel,
        context: ActionExecutionContext
    ): FlowResult<Unit> {
        val attempts = if (step.failurePolicy.name == "RETRY") 3 else 1
        var lastResult: FlowResult<Unit> = FlowResult.Success(Unit)
        repeat(attempts) { attempt ->
            awaitResume()
            lastResult = if (action.type == ActionType.RUN_SUBTASK) {
                executeSubtask(action, context)
            } else {
                actionExecutor.execute(action, context)
            }
            if (lastResult is FlowResult.Success) return lastResult
            if (attempt < attempts - 1) delay(250L * (attempt + 1))
        }
        return lastResult
    }

    private suspend fun executeSubtask(
        action: ActionSpec,
        context: ActionExecutionContext
    ): FlowResult<Unit> {
        val child = taskResolver?.invoke(action.childTaskId, action.childTaskTitle)
            ?: return FlowResult.Failure(
                FlowError(ErrorCode.ACTION_FAILED, "找不到子任务：${action.childTaskTitle ?: action.childTaskId ?: ""}")
            )
        if (child.id in taskStack) {
            return FlowResult.Failure(
                FlowError(ErrorCode.ACTION_FAILED, "检测到子任务递归：${child.title}")
            )
        }
        if (taskStack.size >= 10) {
            return FlowResult.Failure(
                FlowError(ErrorCode.TASK_RECURSION_DETECTED, "子任务嵌套深度超过 10 层：${child.title}")
            )
        }
        taskStack.addLast(child.id)
        return try {
            child.steps.forEach { childStep ->
                awaitResume()
                val conditions = runConditions(childStep, context.copy(taskId = child.id, stepId = childStep.id))
                if (conditions is FlowResult.Failure) return conditions
                val actions = runActions(childStep.actions, childStep, context.copy(taskId = child.id, stepId = childStep.id))
                if (actions is FlowResult.Failure) return actions
            }
            FlowResult.Success(Unit)
        } finally {
            taskStack.removeLastOrNull()
        }
    }

    private fun isNowInWindow(startTime: String?, endTime: String?): Boolean {
        val start = startTime?.toMinutesOrNull() ?: return true
        val end = endTime?.toMinutesOrNull() ?: return true
        val now = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
        return if (start <= end) {
            now in start..end
        } else {
            now >= start || now <= end
        }
    }

    private fun String.toMinutesOrNull(): Int? {
        val parts = split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
}
