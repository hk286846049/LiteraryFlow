package com.monster.literaryflow.ui2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.automation.state.RunState
import com.monster.literaryflow.automation.state.RunStatus
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.ConditionSpec
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.OcrResultModel
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.StepType
import com.monster.literaryflow.core.model.SwipeSpec
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.data.db2.RunRecordEntity2
import com.monster.literaryflow.data.db2.StepRecordEntity2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiteraryFlow2App(
    document: AutomationDocument,
    installedApps: List<InstalledAppOption>,
    runState: RunState,
    runRecords: List<RunRecordEntity2> = emptyList(),
    stepRecords: List<StepRecordEntity2> = emptyList(),
    ocrDebugResult: OcrResultModel? = null,
    ocrDebugBitmap: android.graphics.Bitmap? = null,
    ocrDebugBusy: Boolean = false,
    accessibilityGranted: Boolean = false,
    overlayGranted: Boolean = false,
    screenCaptureGranted: Boolean = false,
    batteryOptimizationGranted: Boolean = false,
    permissionVersion: Int = 0,
    settings: Map<String, Boolean> = emptyMap(),
    numericSettings: Map<String, Int> = emptyMap(),
    loading: Boolean,
    importMessage: String?,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit = {},
    onAddAction: (TaskModel, ActionSpec) -> Unit,
    onAddConfiguredStep: (TaskModel, StepModel) -> Unit = { _, _ -> },
    onUpdateStep: (TaskModel, StepModel) -> Unit = { _, _ -> },
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onCreateTask: () -> Unit,
    onCreateTemplate: (String) -> Unit = { onCreateTask() },
    onDeleteTask: (TaskModel) -> Unit,
    onToggleTaskEnabled: (TaskModel, Boolean) -> Unit = { _, _ -> },
    onToggleTaskFavorite: (TaskModel) -> Unit = {},
    onDuplicateTask: (TaskModel) -> Unit = {},
    onBatchExport: (List<TaskModel>) -> Unit = {},
    onToggleAppFavorite: (String) -> Unit = {},
    onToggleAppOrientation: (String, Boolean) -> Unit = { _, _ -> },
    onSettingChanged: (String, Boolean) -> Unit = { _, _ -> },
    onNumericSettingChanged: (String, Int) -> Unit = { _, _ -> },
    onRunOcrDebug: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onLoadRunDetail: (Long) -> Unit = {},
    onUpdateTask: (TaskModel) -> Unit = {},
    onDeleteStep: (TaskModel, StepModel) -> Unit,
    onSetTargetApp: (TaskModel, String, String) -> Unit,
    onAddWaitStep: (TaskModel) -> Unit,
    onRenameTask: (TaskModel, String) -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onCancelRun: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val permissionStateVersion = permissionVersion
    var currentScreen by remember { mutableStateOf(LfScreen.Home) }
    var selectedTaskId by remember(document.tasks) {
        mutableStateOf(document.tasks.firstOrNull()?.id)
    }
    var lastRunTaskId by remember { mutableStateOf<Long?>(null) }
    var selectedRunRecordId by remember { mutableStateOf<Long?>(null) }
    var runDetailBack by remember { mutableStateOf(LfScreen.Tasks) }
    var moreBack by remember { mutableStateOf(LfScreen.Settings) }
    var permissionBack by remember { mutableStateOf(LfScreen.Home) }
    var importBack by remember { mutableStateOf(LfScreen.Settings) }
    var pendingDeleteTask by remember { mutableStateOf<TaskModel?>(null) }
    var emptyRunTask by remember { mutableStateOf<TaskModel?>(null) }
    var awaitingRunStart by remember { mutableStateOf(false) }
    var isTemplateModalOpen by remember { mutableStateOf(false) }
    var wizardInitialStep by remember { mutableStateOf(1) }

    val selectedTask = document.tasks.firstOrNull { it.id == selectedTaskId }

    val runTaskAndShow = { task: TaskModel ->
        selectedTaskId = task.id
        if (task.steps.isEmpty()) {
            emptyRunTask = task
        } else {
            lastRunTaskId = task.id
            awaitingRunStart = true
            currentScreen = LfScreen.Run
            onRunTask(task)
        }
    }

    LaunchedEffect(document.tasks, currentScreen) {
        if (currentScreen == LfScreen.Wizard && selectedTaskId == null) {
            selectedTaskId = document.tasks.maxByOrNull { it.updatedAt }?.id
        }
    }

    LaunchedEffect(runState.status, runState.taskId) {
        if (currentScreen == LfScreen.Run && runState.status == RunStatus.RUNNING && runState.taskId == lastRunTaskId) {
            awaitingRunStart = false
        }
        if (currentScreen == LfScreen.Run && awaitingRunStart && runState.taskId == lastRunTaskId && runState.status in setOf(RunStatus.COMPLETED, RunStatus.FAILED, RunStatus.CANCELLED)) {
            awaitingRunStart = false
        }
        if (currentScreen == LfScreen.Run && !awaitingRunStart && runState.taskId != null && runState.taskId == lastRunTaskId) {
            if (runState.status == RunStatus.COMPLETED) currentScreen = LfScreen.RunSuccess
            if (runState.status == RunStatus.FAILED || runState.status == RunStatus.CANCELLED) currentScreen = LfScreen.RunFailure
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LfBackground
    ) {
        if (currentScreen == LfScreen.More) {
            ExtendedPagesScreen(
                document = document,
                selectedTaskId = selectedTaskId,
                installedApps = installedApps,
                runState = runState,
                onBack = { currentScreen = moreBack },
                onCreateTask = onCreateTask,
                onCreateTemplate = onCreateTemplate,
                onSetTargetApp = onSetTargetApp,
                onAddAction = onAddAction,
                onRunTask = runTaskAndShow,
                onCancelRun = onCancelRun,
                onPauseRun = onPauseRun,
                onResumeRun = onResumeRun,
                onStartCapture = onStartCapture,
                onOpenOverlaySettings = onOpenOverlaySettings,
                onExportJson = onExportJson,
                onImportJson = onImportJson,
                onUpdateTask = onUpdateTask,
                ocrDebugResult = ocrDebugResult,
                ocrDebugBitmap = ocrDebugBitmap,
                ocrDebugBusy = ocrDebugBusy,
                onRunOcrDebug = onRunOcrDebug
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navbar
                PrototypeNavbar(
                    currentScreen = currentScreen,
                    runningTaskTitle = if (runState.status == RunStatus.RUNNING) (runState.taskTitle ?: selectedTask?.title) else null,
                    runningStepIndex = runState.currentStepIndex,
                    totalRunningSteps = runState.totalSteps,
                    onOpenTemplates = { isTemplateModalOpen = true },
                    onOpenCreateWizard = {
                        selectedTaskId = null
                        wizardInitialStep = 1
                        onCreateTask()
                        currentScreen = LfScreen.Wizard
                    },
                    onOpenPermissions = {
                        permissionBack = currentScreen
                        currentScreen = LfScreen.Permissions
                    },
                    onOpenRunning = {
                        currentScreen = LfScreen.Run
                    }
                )

                // Main Content Viewport
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        LfScreen.Home -> HomeScreenView(
                            document = document,
                            runState = runState,
                            runRecords = runRecords,
                            loading = loading,
                            accessibilityGranted = accessibilityGranted,
                            overlayGranted = overlayGranted,
                            screenCaptureGranted = screenCaptureGranted,
                            batteryOptimizationGranted = batteryOptimizationGranted,
                            onStartCapture = onStartCapture,
                            onStopCapture = onStopCapture,
                            onRunTask = runTaskAndShow,
                            onCancelRun = onCancelRun,
                            onPauseRun = onPauseRun,
                            onResumeRun = onResumeRun,
                            onOpenPermissions = {
                                permissionBack = LfScreen.Home
                                currentScreen = LfScreen.Permissions
                            },
                            onOpenTemplates = { isTemplateModalOpen = true },
                            onCreateTask = {
                                onCreateTask()
                                selectedTaskId = null
                                wizardInitialStep = 1
                                currentScreen = LfScreen.Wizard
                            },
                            onSelectTask = {
                                selectedTaskId = it
                                currentScreen = LfScreen.TaskDetail
                            },
                            onViewAllTasks = { currentScreen = LfScreen.Tasks },
                            onViewAllHistory = { currentScreen = LfScreen.History }
                        )

                        LfScreen.Tasks -> TaskListViewScreen(
                            document = document,
                            selectedTaskId = selectedTaskId,
                            onCreateTask = {
                                onCreateTask()
                                selectedTaskId = null
                                wizardInitialStep = 1
                                currentScreen = LfScreen.Wizard
                            },
                            onOpenTemplates = { isTemplateModalOpen = true },
                            onDeleteTask = onDeleteTask,
                            onRequestDeleteTask = { pendingDeleteTask = it },
                            onRunTask = runTaskAndShow,
                            onToggleTaskEnabled = onToggleTaskEnabled,
                            onToggleTaskFavorite = onToggleTaskFavorite,
                            onDuplicateTask = onDuplicateTask,
                            onEditTask = {
                                selectedTaskId = it.id
                                wizardInitialStep = 1
                                currentScreen = LfScreen.Wizard
                            },
                            onBatchExport = onBatchExport,
                            onSelectTask = {
                                selectedTaskId = it
                                currentScreen = LfScreen.TaskDetail
                            }
                        )

                        LfScreen.TaskDetail -> {
                            if (selectedTask != null) {
                                TaskDetailViewScreen(
                                    task = selectedTask,
                                    records = runRecords,
                                    onBack = { currentScreen = LfScreen.Tasks },
                                    onRunTask = runTaskAndShow,
                                    onEditTask = {
                                        wizardInitialStep = 1
                                        currentScreen = LfScreen.Wizard
                                    },
                                    onEditSteps = {
                                        wizardInitialStep = 3
                                        currentScreen = LfScreen.Wizard
                                    },
                                    onToggleEnable = { onToggleTaskEnabled(selectedTask, !selectedTask.enabled) },
                                    onToggleFavorite = { onToggleTaskFavorite(selectedTask) },
                                    onDuplicateTask = { onDuplicateTask(selectedTask) },
                                    onDeleteTaskRequest = { pendingDeleteTask = selectedTask },
                                    onDeleteStep = { t, s -> onDeleteStep(t, s) },
                                    onSelectRecord = {
                                        selectedRunRecordId = it
                                        runDetailBack = LfScreen.TaskDetail
                                        onLoadRunDetail(it)
                                        currentScreen = LfScreen.RunDetail
                                    }
                                )
                            } else {
                                BentoEmptyState("未选择任务", "返回任务列表选择一个任务查看详情。", "返回任务库", { currentScreen = LfScreen.Tasks })
                            }
                        }

                        LfScreen.Wizard -> CreateTaskWizardScreen(
                            initialTask = selectedTask,
                            allTasks = document.tasks,
                            installedApps = installedApps,
                            ocrResult = ocrDebugResult,
                            initialStep = wizardInitialStep,
                            onStartCapture = onStartCapture,
                            onRunOcrDebug = onRunOcrDebug,
                            onBack = { currentScreen = if (selectedTask != null) LfScreen.TaskDetail else LfScreen.Tasks },
                            onSaveTask = {
                                onUpdateTask(it)
                                selectedTaskId = it.id
                                currentScreen = LfScreen.TaskDetail
                            }
                        )

                        LfScreen.Run -> LiveRunnerViewScreen(
                            task = selectedTask ?: document.tasks.firstOrNull() ?: TaskModel(0L, "待命任务", true, "", "", false, ScheduleType.MANUAL, null, null, null, 1, 0L, 0L, 0L),
                            runState = runState,
                            onPauseRun = onPauseRun,
                            onResumeRun = onResumeRun,
                            onCancelRun = onCancelRun,
                            onSimulateSuccess = { currentScreen = LfScreen.RunSuccess },
                            onSimulateFail = { _ -> currentScreen = LfScreen.RunFailure }
                        )

                        LfScreen.RunSuccess -> RunSuccessViewScreen(
                            task = selectedTask ?: document.tasks.firstOrNull() ?: TaskModel(0L, "已完成任务", true, "", "", false, ScheduleType.MANUAL, null, null, null, 1, 0L, 0L, 0L),
                            durationSec = 42,
                            onRerun = { selectedTask?.let(runTaskAndShow) },
                            onViewDetails = {
                                selectedRunRecordId = runRecords.firstOrNull()?.id
                                currentScreen = LfScreen.RunDetail
                            },
                            onBackToTask = { currentScreen = LfScreen.TaskDetail }
                        )

                        LfScreen.RunFailure -> RunFailureViewScreen(
                            task = selectedTask ?: document.tasks.firstOrNull() ?: TaskModel(0L, "失败任务", true, "", "", false, ScheduleType.MANUAL, null, null, null, 1, 0L, 0L, 0L),
                            failedStepIndex = 4,
                            failureReason = runState.message ?: "在第 4 步寻找“领取”超时未响应 (尝试 3/3 次)",
                            onRerun = { selectedTask?.let(runTaskAndShow) },
                            onEditTask = { currentScreen = LfScreen.Wizard },
                            onViewDetails = {
                                selectedRunRecordId = runRecords.firstOrNull()?.id
                                currentScreen = LfScreen.RunDetail
                            },
                            onBackToTask = { currentScreen = LfScreen.TaskDetail }
                        )

                        LfScreen.RunDetail -> RunRecordDetailTimelineScreen(
                            document = document,
                            records = runRecords,
                            stepRecords = stepRecords,
                            recordId = selectedRunRecordId,
                            onBack = { currentScreen = runDetailBack },
                            onRerun = { selectedTask?.let(runTaskAndShow) }
                        )

                        LfScreen.History -> HistoryListViewScreen(
                            document = document,
                            records = runRecords,
                            onSelectRecord = {
                                selectedRunRecordId = it.id
                                runDetailBack = LfScreen.History
                                onLoadRunDetail(it.id)
                                currentScreen = LfScreen.RunDetail
                            },
                            onClearHistory = onClearHistory,
                            onNavigateToTasks = { currentScreen = LfScreen.Tasks }
                        )

                        LfScreen.Apps -> AppListViewScreen(
                            installedApps = installedApps,
                            document = document,
                            onToggleAppFavorite = onToggleAppFavorite,
                            onToggleAppOrientation = onToggleAppOrientation,
                            onCreateTaskForApp = { appName, pkg ->
                                onSetTargetApp(TaskModel(0L, "$appName 自动化", true, pkg, appName, true, ScheduleType.DAILY, "09:00", "23:00", null, 1, 0L, 0L, 0L), appName, pkg)
                                wizardInitialStep = 1
                                currentScreen = LfScreen.Wizard
                            },
                            onSelectTask = {
                                selectedTaskId = it
                                currentScreen = LfScreen.TaskDetail
                            }
                        )

                        LfScreen.Settings -> SettingsViewScreen(
                            settings = settings,
                            numericSettings = numericSettings,
                            onSettingChanged = onSettingChanged,
                            onNumericSettingChanged = onNumericSettingChanged,
                            onClearHistory = onClearHistory,
                            onExportJson = onExportJson,
                            onImportJson = {
                                importBack = LfScreen.Settings
                                currentScreen = LfScreen.Import
                            },
                            onOpenPermissions = {
                                permissionBack = LfScreen.Settings
                                currentScreen = LfScreen.Permissions
                            },
                            onOpenMoreTools = {
                                moreBack = LfScreen.Settings
                                currentScreen = LfScreen.More
                            }
                        )

                        LfScreen.Permissions -> PermissionCenterViewScreen(
                            accessibilityGranted = accessibilityGranted,
                            overlayGranted = overlayGranted,
                            screenCaptureGranted = screenCaptureGranted,
                            batteryOptimizationGranted = batteryOptimizationGranted,
                            onBack = { currentScreen = permissionBack },
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            onOpenOverlaySettings = onOpenOverlaySettings,
                            onOpenBatterySettings = onOpenBatterySettings,
                            onStartCapture = onStartCapture
                        )

                        LfScreen.Import -> ImportScreen(
                            importMessage = importMessage,
                            onBack = { currentScreen = importBack },
                            onExportJson = onExportJson,
                            onImportJson = onImportJson
                        )

                        LfScreen.Editor -> Unit
                        LfScreen.More -> Unit
                    }
                }

                // Mobile Floating Bottom Navigation Bar
                PrototypeBottomNav(
                    currentScreen = currentScreen,
                    onSelectScreen = { currentScreen = it }
                )
            }
        }

        // Global Modals & Dialogs
        TaskTemplateModal(
            isOpen = isTemplateModalOpen,
            onClose = { isTemplateModalOpen = false },
            onApplyTemplate = { tpl ->
                val newSteps = tpl.steps.mapIndexed { idx, stepName ->
                    StepModel(
                        id = System.currentTimeMillis() + idx,
                        taskId = 0L,
                        orderIndex = idx,
                        type = if (stepName.contains("等待")) StepType.CONDITION else StepType.ACTION,
                        title = stepName,
                        timeoutMs = if (stepName.contains("等待")) 15000L else 5000L,
                        delayAfterMs = 1000L,
                        failurePolicy = FailurePolicy.STOP,
                        actions = listOf(ActionSpec(type = if (stepName.contains("等待")) ActionType.WAIT_FOR_TEXT else ActionType.TAP_TEXT, text = if (stepName.contains("签到")) "签到" else if (stepName.contains("领取")) "领取" else null))
                    )
                }
                val newTask = TaskModel(
                    id = System.currentTimeMillis(),
                    title = tpl.name,
                    enabled = true,
                    targetPackageName = "com.hypergryph.arknights",
                    targetAppName = "明日方舟",
                    allowAutoOpenApp = true,
                    scheduleType = ScheduleType.DAILY,
                    startTime = "09:00",
                    endTime = "23:00",
                    weeklyDays = "1,2,3,4,5,6,7",
                    runTimes = 1,
                    intervalMs = 1800000L,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    steps = newSteps,
                    category = tpl.category,
                    favorite = false,
                    estimatedDurationMs = (newSteps.size * 12 + 15) * 1000L,
                    todayRunCount = 0
                )
                onUpdateTask(newTask)
                selectedTaskId = newTask.id
                currentScreen = LfScreen.TaskDetail
            }
        )

        pendingDeleteTask?.let { task ->
            BentoConfirmDialog(
                isOpen = true,
                title = "删除任务 “${task.title}”？",
                description = "删除后此任务的步骤配置将无法恢复，确认删除？",
                isDestructive = true,
                confirmText = "确认删除",
                onConfirm = {
                    onDeleteTask(task)
                    pendingDeleteTask = null
                    if (selectedTaskId == task.id) selectedTaskId = null
                    currentScreen = LfScreen.Tasks
                },
                onDismiss = { pendingDeleteTask = null }
            )
        }

        emptyRunTask?.let { task ->
            BentoConfirmDialog(
                isOpen = true,
                title = "任务还没有编排步骤",
                description = "“${task.title}”当前没有可执行步骤，请先进入向导添加动作或等待条件。",
                confirmText = "去编辑向导",
                onConfirm = {
                    emptyRunTask = null
                    currentScreen = LfScreen.Wizard
                },
                onDismiss = { emptyRunTask = null }
            )
        }
    }
}

// ==========================================
// Top Navigation Bar
// ==========================================
@Composable
private fun PrototypeNavbar(
    currentScreen: LfScreen,
    runningTaskTitle: String?,
    runningStepIndex: Int,
    totalRunningSteps: Int,
    onOpenTemplates: () -> Unit,
    onOpenCreateWizard: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenRunning: () -> Unit
) {
    Surface(
        color = LfBackground.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Title & Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LfEmerald500),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 18.sp)
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            when (currentScreen) {
                                LfScreen.Home -> "文流 2.0"
                                LfScreen.Tasks -> "自动化任务"
                                LfScreen.TaskDetail -> "任务详情与编排"
                                LfScreen.Wizard -> "任务向导"
                                LfScreen.Run -> "执行控制台"
                                LfScreen.RunSuccess -> "任务完成"
                                LfScreen.RunFailure -> "执行异常"
                                LfScreen.RunDetail -> "运行日志记录"
                                LfScreen.History -> "运行日志"
                                LfScreen.Apps -> "目标应用库"
                                LfScreen.Permissions -> "系统权限中心"
                                LfScreen.Settings -> "偏好与设置"
                                else -> "文流 2.0"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = LfEmerald800
                        )
                        BentoBadge("Bento v2.0", LfEmerald50, LfEmerald700, LfEmerald200)
                    }
                    Text("视觉感知自动化系统", fontSize = 10.sp, color = LfSlate500)
                }
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (runningTaskTitle != null) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = LfEmerald50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LfEmerald300),
                        modifier = Modifier.clickable { onOpenRunning() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PulseDot(LfEmerald500, 6.dp)
                            Text("▶ $runningTaskTitle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfEmerald800, maxLines = 1)
                            Text("$runningStepIndex/$totalRunningSteps", fontSize = 10.sp, color = LfEmerald600)
                        }
                    }
                }

                BentoBadge("✨ 模板库", LfEmerald50, LfEmerald700, LfEmerald200, modifier = Modifier.clickable { onOpenTemplates() })

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, LfSlate200, CircleShape)
                        .clickable { onOpenPermissions() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================
// Bottom Navigation Bar
// ==========================================
enum class LfScreen(val label: String, val symbol: String) {
    Home("首页", "🏠"),
    Tasks("任务", "📑"),
    History("记录", "⏱️"),
    Apps("应用", "📱"),
    Settings("设置", "⚙️"),
    TaskDetail("详情", "🔍"),
    Wizard("向导", "✏️"),
    Run("执行", "▶"),
    RunSuccess("成功", "✓"),
    RunFailure("失败", "!"),
    RunDetail("日志详情", "📜"),
    Permissions("权限", "🛡️"),
    Import("导入", "⇩"),
    Editor("编排", "＋"),
    More("更多", "⋯")
}

@Composable
private fun PrototypeBottomNav(
    currentScreen: LfScreen,
    onSelectScreen: (LfScreen) -> Unit
) {
    val navItems = listOf(
        LfScreen.Home,
        LfScreen.Tasks,
        LfScreen.History,
        LfScreen.Apps,
        LfScreen.Settings
    )

    Surface(
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { screen ->
                val isActive = when (screen) {
                    LfScreen.Home -> currentScreen == LfScreen.Home
                    LfScreen.Tasks -> currentScreen in setOf(LfScreen.Tasks, LfScreen.TaskDetail, LfScreen.Wizard, LfScreen.Run, LfScreen.RunSuccess, LfScreen.RunFailure)
                    LfScreen.History -> currentScreen in setOf(LfScreen.History, LfScreen.RunDetail)
                    LfScreen.Apps -> currentScreen == LfScreen.Apps
                    LfScreen.Settings -> currentScreen in setOf(LfScreen.Settings, LfScreen.Permissions, LfScreen.Import, LfScreen.More)
                    else -> currentScreen == screen
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectScreen(screen) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isActive) LfEmerald50 else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(screen.symbol, fontSize = if (isActive) 18.sp else 16.sp)
                    }
                    Text(
                        screen.label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) LfEmerald700 else LfSlate400
                    )
                }
            }
        }
    }
}

// ==========================================
// Import Task Screen
// ==========================================
@Composable
private fun ImportScreen(
    importMessage: String?,
    onBack: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BentoOutlinedButton(text = "‹ 返回设置", onClick = onBack, modifier = Modifier.height(36.dp))
                    Text("导入与备份任务", fontSize = 15.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                    Spacer(Modifier.width(36.dp))
                }
            }
        }

        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("粘贴任务配置 JSON", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                    Text("支持 2.0 Schema 或 1.0 旧版 AutoInfo 数组 JSON", fontSize = 11.sp, color = LfSlate500)

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("[{\"name\": \"签到\", \"steps\": [...]}]") },
                        minLines = 8,
                        maxLines = 14,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoOutlinedButton(
                            text = "导出当前任务",
                            onClick = onExportJson,
                            modifier = Modifier.weight(1f).height(42.dp)
                        )
                        BentoButton(
                            text = "转换并导入",
                            onClick = { onImportJson(text) },
                            backgroundColor = LfEmerald500,
                            modifier = Modifier.weight(1f).height(42.dp)
                        )
                    }

                    if (importMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LfEmerald50)
                                .border(1.dp, LfEmerald200, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(importMessage, fontSize = 11.sp, color = LfEmerald800, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
