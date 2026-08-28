package com.monster.literaryflow.core.model

data class AutomationDocument(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val tasks: List<TaskModel> = emptyList(),
    val appProfiles: List<AppProfileModel> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

data class TaskModel(
    val id: Long,
    val title: String,
    val enabled: Boolean,
    val targetPackageName: String,
    val targetAppName: String,
    val allowAutoOpenApp: Boolean,
    val scheduleType: ScheduleType,
    val startTime: String?,
    val endTime: String?,
    val weeklyDays: String?,
    val runTimes: Int,
    val intervalMs: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val schemaVersion: Int = AutomationDocument.CURRENT_SCHEMA_VERSION,
    val steps: List<StepModel> = emptyList(),
    val favorite: Boolean = false,
    val category: String = "",
    val estimatedDurationMs: Long = 0L,
    val todayRunCount: Int = 0
)

data class StepModel(
    val id: Long,
    val taskId: Long,
    val orderIndex: Int,
    val type: StepType,
    val title: String,
    val timeoutMs: Long,
    val delayAfterMs: Long,
    val failurePolicy: FailurePolicy,
    val actions: List<ActionSpec> = emptyList(),
    val conditions: List<ConditionSpec> = emptyList(),
    /** 步骤备注说明（可选），仅用于向导与详情展示 */
    val description: String? = null,
    /** 执行该步骤前的固定等待（毫秒） */
    val waitBeforeMs: Long = 0L,
    /** failurePolicy == RETRY 时的最大重试次数 */
    val retryCount: Int = 3
)

data class ActionSpec(
    val type: ActionType,
    val excludedText: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val text: String? = null,
    val matchType: MatchType? = null,
    val recognitionSource: RecognitionSource? = null,
    val swipe: SwipeSpec? = null,
    val inputText: String? = null,
    val childTaskId: Long? = null,
    val childTaskTitle: String? = null,
    val targetPackageName: String? = null,
    val targetAppName: String? = null,
    val roi: NormalizedRect? = null,
    val timeoutMs: Long = 3000L,
    val repeatCount: Int = 1,
    val legacyNote: String? = null
)

data class ConditionSpec(
    val type: ConditionType,
    val text: String? = null,
    val excludedText: String? = null,
    val matchType: MatchType? = null,
    val recognitionSource: RecognitionSource? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val timeoutMs: Long = 3000L,
    val checkIntervalMs: Long = 1000L,
    val onTrueActions: List<ActionSpec> = emptyList(),
    val onFalseActions: List<ActionSpec> = emptyList(),
    val legacyNote: String? = null
)

data class SwipeSpec(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long = 400L
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun normalized(): NormalizedRect {
        val safeLeft = left.coerceIn(0f, 1f)
        val safeTop = top.coerceIn(0f, 1f)
        val safeRight = right.coerceIn(safeLeft, 1f)
        val safeBottom = bottom.coerceIn(safeTop, 1f)
        return copy(left = safeLeft, top = safeTop, right = safeRight, bottom = safeBottom)
    }
}

data class OcrBlockModel(
    val text: String,
    val confidence: Float,
    val boundingBox: IntRect,
    val normalizedBox: NormalizedRect
)

data class OcrResultModel(
    val frameId: Long,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val blocks: List<OcrBlockModel>
)

data class IntRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class ScreenStateModel(
    val id: Long,
    val packageName: String,
    val title: String?,
    val signature: String,
    val timestamp: Long,
    val ocrResult: OcrResultModel?,
    val stable: Boolean
)

data class AppProfileModel(
    val id: Long,
    val packageName: String,
    val appName: String,
    val defaultRecognitionSource: RecognitionSource,
    val defaultRoi: NormalizedRect? = null,
    val landscape: Boolean = false,
    val allowShizuku: Boolean = false,
    val favorite: Boolean = false
)

enum class ScheduleType {
    MANUAL,
    DAILY,
    WEEKLY,
    LOOP,
    RUN_COUNT
}

enum class StepType {
    ACTION,
    CONDITION,
    SUBTASK,
    WAIT
}

enum class FailurePolicy {
    STOP,
    RETRY,
    SKIP
}

enum class ActionType {
    TAP_COORDINATE,
    TAP_TEXT,
    SWIPE,
    INPUT_TEXT,
    BACK,
    OPEN_APP,
    LONG_PRESS,
    RUN_SUBTASK,
    WAIT,
    WAIT_FOR_TEXT,
    WAIT_FOR_SCREEN
}

enum class ConditionType {
    TEXT_VISIBLE,
    TIME_WINDOW,
    SCREEN_STATE
}

enum class MatchType {
    EXACT,
    FUZZY,
    ANY_KEYWORD
}

enum class RecognitionSource {
    ACCESSIBILITY,
    OCR,
    TEMPLATE,
    AUTO
}

enum class PerceptionMode {
    IDLE,
    NORMAL,
    FAST
}
