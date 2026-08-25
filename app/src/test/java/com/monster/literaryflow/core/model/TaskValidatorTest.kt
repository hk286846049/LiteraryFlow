package com.monster.literaryflow.core.model

import org.junit.Assert.assertTrue
import org.junit.Test

class TaskValidatorTest {
    @Test
    fun rejectsInvalidNormalizedCoordinatesAndDuplicateIds() {
        val task = TaskModel(
            id = 1L,
            title = "任务",
            enabled = true,
            targetPackageName = "",
            targetAppName = "",
            allowAutoOpenApp = false,
            scheduleType = ScheduleType.MANUAL,
            startTime = null,
            endTime = null,
            weeklyDays = null,
            runTimes = 1,
            intervalMs = 0L,
            createdAt = 0L,
            updatedAt = 0L,
            steps = listOf(
                StepModel(
                    id = 2L,
                    taskId = 1L,
                    orderIndex = 0,
                    type = StepType.ACTION,
                    title = "点击",
                    timeoutMs = 1000L,
                    delayAfterMs = 0L,
                    failurePolicy = FailurePolicy.STOP,
                    actions = listOf(ActionSpec(type = ActionType.TAP_COORDINATE, x = 2f, y = -1f))
                )
            )
        )
        val issues = TaskValidator.validate(AutomationDocument(tasks = listOf(task, task)))
        assertTrue(issues.any { it.message.contains("任务 id 重复") })
        assertTrue(issues.any { it.message.contains("x 必须") })
        assertTrue(issues.any { it.message.contains("y 必须") })
    }
}
