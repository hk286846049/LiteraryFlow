package com.monster.literaryflow.data.db2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepType

@Entity(tableName = "lf2_tasks")
data class TaskEntity2(
    @PrimaryKey val id: Long,
    val title: String,
    val enabled: Boolean,
    @ColumnInfo(name = "target_package_name") val targetPackageName: String,
    @ColumnInfo(name = "target_app_name") val targetAppName: String,
    @ColumnInfo(name = "allow_auto_open_app") val allowAutoOpenApp: Boolean,
    @ColumnInfo(name = "schedule_type") val scheduleType: ScheduleType,
    @ColumnInfo(name = "start_time") val startTime: String?,
    @ColumnInfo(name = "end_time") val endTime: String?,
    @ColumnInfo(name = "weekly_days") val weeklyDays: String?,
    @ColumnInfo(name = "run_times") val runTimes: Int,
    @ColumnInfo(name = "interval_ms") val intervalMs: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    val favorite: Boolean = false,
    val category: String = "",
    @ColumnInfo(name = "estimated_duration_ms") val estimatedDurationMs: Long = 0L,
    @ColumnInfo(name = "today_run_count") val todayRunCount: Int = 0
)

@Entity(
    tableName = "lf2_steps",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity2::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("task_id")]
)
data class StepEntity2(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    val type: StepType,
    val title: String,
    @ColumnInfo(name = "timeout_ms") val timeoutMs: Long,
    @ColumnInfo(name = "delay_after_ms") val delayAfterMs: Long,
    @ColumnInfo(name = "failure_policy") val failurePolicy: FailurePolicy
)

@Entity(
    tableName = "lf2_actions",
    foreignKeys = [
        ForeignKey(
            entity = StepEntity2::class,
            parentColumns = ["id"],
            childColumns = ["step_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("step_id")]
)
data class ActionEntity2(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "step_id") val stepId: Long,
    val type: ActionType,
    val x: Float?,
    val y: Float?,
    val text: String?,
    @ColumnInfo(name = "match_type") val matchType: MatchType?,
    @ColumnInfo(name = "recognition_source") val recognitionSource: RecognitionSource?,
    @ColumnInfo(name = "swipe_json") val swipeJson: String?,
    @ColumnInfo(name = "input_text") val inputText: String?,
    @ColumnInfo(name = "child_task_id") val childTaskId: Long?,
    @ColumnInfo(name = "child_task_title") val childTaskTitle: String?,
    @ColumnInfo(name = "target_package_name") val targetPackageName: String?,
    @ColumnInfo(name = "target_app_name") val targetAppName: String?,
    @ColumnInfo(name = "roi_json") val roiJson: String?,
    @ColumnInfo(name = "timeout_ms") val timeoutMs: Long,
    @ColumnInfo(name = "repeat_count") val repeatCount: Int,
    @ColumnInfo(name = "legacy_note") val legacyNote: String?
)

@Entity(
    tableName = "lf2_conditions",
    foreignKeys = [
        ForeignKey(
            entity = StepEntity2::class,
            parentColumns = ["id"],
            childColumns = ["step_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("step_id")]
)
data class ConditionEntity2(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "step_id") val stepId: Long,
    val type: ConditionType,
    val text: String?,
    @ColumnInfo(name = "excluded_text") val excludedText: String?,
    @ColumnInfo(name = "match_type") val matchType: MatchType?,
    @ColumnInfo(name = "recognition_source") val recognitionSource: RecognitionSource?,
    @ColumnInfo(name = "start_time") val startTime: String?,
    @ColumnInfo(name = "end_time") val endTime: String?,
    @ColumnInfo(name = "timeout_ms") val timeoutMs: Long,
    @ColumnInfo(name = "check_interval_ms") val checkIntervalMs: Long,
    @ColumnInfo(name = "on_true_actions_json") val onTrueActionsJson: String,
    @ColumnInfo(name = "on_false_actions_json") val onFalseActionsJson: String,
    @ColumnInfo(name = "legacy_note") val legacyNote: String?
)

@Entity(tableName = "lf2_run_records")
data class RunRecordEntity2(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    val status: String,
    val message: String?
)

@Entity(tableName = "lf2_step_records")
data class StepRecordEntity2(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "run_record_id") val runRecordId: Long,
    @ColumnInfo(name = "step_id") val stepId: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    val status: String,
    val message: String?
)

@Entity(tableName = "lf2_app_profiles")
data class AppProfileEntity2(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "default_recognition_source") val defaultRecognitionSource: RecognitionSource,
    @ColumnInfo(name = "default_roi_json") val defaultRoiJson: String?,
    val landscape: Boolean,
    @ColumnInfo(name = "allow_shizuku") val allowShizuku: Boolean,
    val favorite: Boolean = false
)
