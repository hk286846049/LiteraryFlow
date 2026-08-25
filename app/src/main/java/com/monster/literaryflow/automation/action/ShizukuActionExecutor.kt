package com.monster.literaryflow.automation.action

import android.content.Context
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.result.ErrorCode
import com.monster.literaryflow.core.result.FlowError
import com.monster.literaryflow.core.result.FlowResult

interface ShizukuGateway {
    fun isAvailable(): Boolean
    suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit>
}

class UnsupportedShizukuGateway : ShizukuGateway {
    override fun isAvailable(): Boolean = false

    override suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit> {
        return FlowResult.Failure(
            FlowError(ErrorCode.ACTION_FAILED, "Shizuku 通道不可用，已回退到 Accessibility")
        )
    }
}

class OptionalShizukuActionExecutor(
    private val fallback: ActionExecutor,
    private val shizuku: ShizukuGateway = UnsupportedShizukuGateway()
) : ActionExecutor {
    override suspend fun execute(action: ActionSpec, context: ActionExecutionContext): FlowResult<Unit> {
        return if (shizuku.isAvailable() && action.usesPrivilegedChannel()) {
            when (val result = shizuku.execute(action, context)) {
                is FlowResult.Success -> result
                is FlowResult.Failure -> fallback.execute(action, context)
            }
        } else {
            fallback.execute(action, context)
        }
    }

    private fun ActionSpec.usesPrivilegedChannel(): Boolean {
        return type.name in setOf("TAP_COORDINATE", "SWIPE", "LONG_PRESS", "INPUT_TEXT")
    }
}
