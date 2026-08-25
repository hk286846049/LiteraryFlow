package com.monster.literaryflow.automation.state

import com.monster.literaryflow.core.model.PerceptionMode

data class RunState(
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val status: RunStatus = RunStatus.IDLE,
    val perceptionMode: PerceptionMode = PerceptionMode.IDLE,
    val message: String? = null
)

enum class RunStatus {
    IDLE,
    RUNNING,
    PAUSED,
    CANCELLING,
    COMPLETED,
    FAILED,
    CANCELLED
}
