package com.monster.literaryflow.overlay

import com.monster.literaryflow.automation.state.RunStatus

data class OverlayState2(
    val visible: Boolean = false,
    val expanded: Boolean = false,
    val taskTitle: String? = null,
    val runStatus: RunStatus = RunStatus.IDLE,
    val progressText: String = "待机",
    val lastOcrText: String? = null
)
