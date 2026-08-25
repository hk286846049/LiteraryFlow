package com.monster.literaryflow.vision.ocr

import com.monster.literaryflow.core.model.PerceptionMode

class AdaptiveOcrScheduler {
    var mode: PerceptionMode = PerceptionMode.IDLE
        private set

    fun onTaskStarted() {
        mode = PerceptionMode.NORMAL
    }

    fun onScreenEntered() {
        mode = PerceptionMode.FAST
    }

    fun onStableFrame() {
        mode = PerceptionMode.NORMAL
    }

    fun onTaskFinished() {
        mode = PerceptionMode.IDLE
    }

    fun nextDelayMs(): Long = when (mode) {
        PerceptionMode.IDLE -> Long.MAX_VALUE
        PerceptionMode.NORMAL -> 1000L
        PerceptionMode.FAST -> 250L
    }
}
