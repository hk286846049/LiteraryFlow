package com.monster.literaryflow.automation.action

import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.result.FlowResult

interface ActionExecutor {
    suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit>
}

data class ActionExecutionContext(
    val taskId: Long,
    val stepId: Long,
    val screenWidth: Int,
    val screenHeight: Int,
    val packageName: String,
    val autoOpenTargetApp: Boolean = false,
    val screenMetrics: com.monster.literaryflow.core.model.ScreenMetrics =
        com.monster.literaryflow.core.model.ScreenMetrics(screenWidth, screenHeight)
)

class NoOpActionExecutor : ActionExecutor {
    override suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit> {
        return FlowResult.Success(Unit)
    }
}
