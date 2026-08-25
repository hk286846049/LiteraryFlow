package com.monster.literaryflow.automation.engine

import com.monster.literaryflow.automation.action.ActionExecutionContext
import com.monster.literaryflow.automation.action.ActionExecutor
import com.monster.literaryflow.automation.state.RunStatus
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.ConditionSpec
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.StepType
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.result.ErrorCode
import com.monster.literaryflow.core.result.FlowError
import com.monster.literaryflow.core.result.FlowResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionEngineTest {
    @Test
    fun textConditionRunsTrueBranchAndMainActions() = runBlocking {
        val executor = RecordingExecutor(successText = "签到")
        val engine = ExecutionEngine(executor, CoroutineScope(coroutineContext + SupervisorJob()))
        val task = taskWithCondition(
            conditionText = "签到",
            onTrue = listOf(ActionSpec(type = ActionType.TAP_TEXT, text = "领取")),
            main = listOf(ActionSpec(type = ActionType.BACK))
        )

        engine.start(task, context(task.id))
        engine.awaitTerminalState()

        assertEquals(RunStatus.COMPLETED, engine.state.value.status)
        assertEquals(
            listOf(ActionType.WAIT_FOR_TEXT, ActionType.TAP_TEXT, ActionType.BACK),
            executor.calls.map { it.type }
        )
    }

    @Test
    fun textConditionFailureRunsFalseBranch() = runBlocking {
        val executor = RecordingExecutor(successText = "签到")
        val engine = ExecutionEngine(executor, CoroutineScope(coroutineContext + SupervisorJob()))
        val task = taskWithCondition(
            conditionText = "未出现",
            onFalse = listOf(ActionSpec(type = ActionType.WAIT, timeoutMs = 1L))
        )

        engine.start(task, context(task.id))
        engine.awaitTerminalState()

        assertEquals(RunStatus.COMPLETED, engine.state.value.status)
        assertEquals(
            listOf(ActionType.WAIT_FOR_TEXT, ActionType.WAIT),
            executor.calls.map { it.type }
        )
    }

    @Test
    fun retryPolicyRetriesFailedAction() = runBlocking {
        val executor = RecordingExecutor(successText = "unused", failuresBeforeSuccess = 2)
        val engine = ExecutionEngine(executor, CoroutineScope(coroutineContext + SupervisorJob()))
        val task = taskWithCondition(
            conditionText = "unused",
            main = listOf(ActionSpec(type = ActionType.BACK))
        ).copy(
            steps = taskWithCondition(
                conditionText = "unused",
                main = listOf(ActionSpec(type = ActionType.BACK))
            ).steps.map { it.copy(failurePolicy = FailurePolicy.RETRY, conditions = emptyList()) }
        )

        engine.start(task, context(task.id))
        engine.awaitTerminalState()

        assertEquals(RunStatus.COMPLETED, engine.state.value.status)
        assertEquals(3, executor.calls.size)
    }

    @Test
    fun recursiveSubtaskFailsWithReadableError() = runBlocking {
        val executor = RecordingExecutor(successText = "unused")
        var child: TaskModel? = null
        val engine = ExecutionEngine(
            actionExecutor = executor,
            scope = CoroutineScope(coroutineContext + SupervisorJob()),
            taskResolver = { _, _ -> child }
        )
        val parent = taskWithCondition(
            conditionText = "unused",
            main = listOf(ActionSpec(type = ActionType.RUN_SUBTASK, childTaskId = 200L))
        )
        child = parent.copy(id = 200L, title = "子任务")

        engine.start(parent, context(parent.id))
        engine.awaitTerminalState()

        assertEquals(RunStatus.FAILED, engine.state.value.status)
        assertTrue(engine.state.value.message?.contains("递归") == true)
    }

    @Test
    fun excludedPageTextFailsCondition() = runBlocking {
        val executor = object : ActionExecutor {
            override suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit> {
                return if (action.type == ActionType.WAIT_FOR_TEXT && action.text == "错误页") {
                    FlowResult.Success(Unit)
                } else {
                    FlowResult.Success(Unit)
                }
            }
        }
        val engine = ExecutionEngine(executor, CoroutineScope(coroutineContext + SupervisorJob()))
        val task = taskWithCondition(conditionText = "首页").copy(
            steps = taskWithCondition(conditionText = "首页").steps.map { step ->
                step.copy(conditions = step.conditions.map { it.copy(excludedText = "错误页") })
            }
        )
        engine.start(task, context(task.id))
        engine.awaitTerminalState()

        assertEquals(RunStatus.FAILED, engine.state.value.status)
        assertTrue(engine.state.value.message?.contains("排除词") == true)
    }

    private suspend fun ExecutionEngine.awaitTerminalState() {
        withTimeout(1000L) {
            while (state.value.status !in terminalStates) {
                delay(10L)
            }
        }
    }

    private fun taskWithCondition(
        conditionText: String,
        onTrue: List<ActionSpec> = emptyList(),
        onFalse: List<ActionSpec> = emptyList(),
        main: List<ActionSpec> = emptyList()
    ): TaskModel {
        val taskId = 100L
        return TaskModel(
            id = taskId,
            title = "测试任务",
            enabled = true,
            targetPackageName = "com.example",
            targetAppName = "Example",
            allowAutoOpenApp = true,
            scheduleType = ScheduleType.MANUAL,
            startTime = null,
            endTime = null,
            weeklyDays = null,
            runTimes = 1,
            intervalMs = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            steps = listOf(
                StepModel(
                    id = 1L,
                    taskId = taskId,
                    orderIndex = 0,
                    type = StepType.CONDITION,
                    title = "等待文字",
                    timeoutMs = 1000L,
                    delayAfterMs = 0L,
                    failurePolicy = FailurePolicy.STOP,
                    conditions = listOf(
                        ConditionSpec(
                            type = ConditionType.TEXT_VISIBLE,
                            text = conditionText,
                            matchType = MatchType.EXACT,
                            timeoutMs = 1000L,
                            onTrueActions = onTrue,
                            onFalseActions = onFalse
                        )
                    ),
                    actions = main
                )
            )
        )
    }

    private fun context(taskId: Long): ActionExecutionContext = ActionExecutionContext(
        taskId = taskId,
        stepId = 0L,
        screenWidth = 1080,
        screenHeight = 1920,
        packageName = "com.example"
    )

    private class RecordingExecutor(
        private val successText: String,
        private var failuresBeforeSuccess: Int = 0
    ) : ActionExecutor {
        val calls = mutableListOf<ActionSpec>()

        override suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit> {
            calls += action
            if (failuresBeforeSuccess > 0 && action.type != ActionType.WAIT_FOR_TEXT) {
                failuresBeforeSuccess -= 1
                return FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "transient"))
            }
            return if (action.type == ActionType.WAIT_FOR_TEXT && action.text != successText) {
                FlowResult.Failure(FlowError(ErrorCode.ACTION_FAILED, "missing text"))
            } else {
                FlowResult.Success(Unit)
            }
        }
    }

    private companion object {
        val terminalStates = setOf(
            RunStatus.COMPLETED,
            RunStatus.FAILED,
            RunStatus.CANCELLED
        )
    }
}
