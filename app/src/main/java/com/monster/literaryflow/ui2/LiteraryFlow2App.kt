package com.monster.literaryflow.ui2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.ActionSpec
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
import com.monster.literaryflow.automation.state.RunState
import com.monster.literaryflow.automation.state.RunStatus
import com.monster.literaryflow.data.db2.RunRecordEntity2

@Composable
fun LiteraryFlow2App(
    document: AutomationDocument,
    installedApps: List<InstalledAppOption>,
    runState: RunState,
    runRecords: List<RunRecordEntity2> = emptyList(),
    stepRecords: List<com.monster.literaryflow.data.db2.StepRecordEntity2> = emptyList(),
    ocrDebugResult: com.monster.literaryflow.core.model.OcrResultModel? = null,
    ocrDebugBitmap: android.graphics.Bitmap? = null,
    ocrDebugBusy: Boolean = false,
    accessibilityGranted: Boolean = false,
    overlayGranted: Boolean = false,
    screenCaptureGranted: Boolean = false,
    settings: Map<String, Boolean> = emptyMap(),
    numericSettings: Map<String, Int> = emptyMap(),
    loading: Boolean,
    importMessage: String?,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
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
    var screen by remember { mutableStateOf(LfScreen.Home) }
    var selectedTaskId by remember(document.tasks) {
        mutableStateOf(document.tasks.firstOrNull()?.id)
    }
    var lastRunTaskId by remember { mutableStateOf<Long?>(null) }
    var selectedRunRecordId by remember { mutableStateOf<Long?>(null) }
    var awaitingRunStart by remember { mutableStateOf(false) }
    val selectedTask = document.tasks.firstOrNull { it.id == selectedTaskId }
    val runTaskAndShow = { task: TaskModel ->
        selectedTaskId = task.id
        if (task.steps.isEmpty()) {
            onRunTask(task)
        } else {
            lastRunTaskId = task.id
            awaitingRunStart = true
            screen = LfScreen.Run
            onRunTask(task)
        }
    }
    LaunchedEffect(document.tasks, screen) {
        if (screen == LfScreen.Wizard && selectedTaskId == null) {
            selectedTaskId = document.tasks.maxByOrNull { it.updatedAt }?.id
        }
    }
    LaunchedEffect(runState.status, runState.taskId) {
        if (screen == LfScreen.Run && runState.status == RunStatus.RUNNING && runState.taskId == lastRunTaskId) {
            awaitingRunStart = false
        }
        if (screen == LfScreen.Run && !awaitingRunStart && runState.taskId != null && runState.taskId == lastRunTaskId) {
            if (runState.status == RunStatus.COMPLETED) screen = LfScreen.RunSuccess
            if (runState.status == RunStatus.FAILED || runState.status == RunStatus.CANCELLED) screen = LfScreen.RunFailure
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LfBackground
    ) {
        if (screen == LfScreen.More) {
            ExtendedPagesScreen(
                document = document,
                installedApps = installedApps,
                runState = runState,
                onBack = { screen = LfScreen.Home },
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
                Box(modifier = Modifier.weight(1f)) {
                    when (screen) {
                    LfScreen.Home -> HomeScreen(document, runState, loading, onStartCapture, onStopCapture, runTaskAndShow, onCancelRun)
                    LfScreen.Tasks -> TasksScreen(document, selectedTaskId, onCreateTask = { onCreateTask(); selectedTaskId = null; screen = LfScreen.Wizard }, onDeleteTask = onDeleteTask, onRunTask = runTaskAndShow, onToggleTaskEnabled = onToggleTaskEnabled, onToggleTaskFavorite = onToggleTaskFavorite, onDuplicateTask = onDuplicateTask, onBatchExport = onBatchExport, onSelect = { selectedTaskId = it; screen = LfScreen.TaskDetail })
                    LfScreen.Editor -> EditorScreen(
                        task = document.tasks.firstOrNull { it.id == selectedTaskId } ?: document.tasks.firstOrNull(),
                        onRunTask = runTaskAndShow,
                        onAddWaitStep = onAddWaitStep,
                        onRenameTask = onRenameTask,
                        onDeleteStep = onDeleteStep
                    )
                    LfScreen.TaskDetail -> if (selectedTask != null) TaskDetailScreen(selectedTask, runRecords, onBack = { screen = LfScreen.Tasks }, onRun = runTaskAndShow, onEdit = { screen = LfScreen.Wizard }, onToggle = { onToggleTaskEnabled(selectedTask, !selectedTask.enabled) }, onDuplicate = { onDuplicateTask(selectedTask) }, onDelete = { onDeleteTask(selectedTask); screen = LfScreen.Tasks }, onSelectRecord = { selectedRunRecordId = it; onLoadRunDetail(it); screen = LfScreen.RunDetail }) else EmptyCard("未选择任务", "返回任务列表选择一个任务。")
                    LfScreen.Wizard -> TaskWizardScreen(selectedTask, document.tasks, installedApps, ocrDebugResult, onStartCapture, onRunOcrDebug, onBack = { screen = LfScreen.TaskDetail }, onSave = { onUpdateTask(it); screen = LfScreen.TaskDetail }, onSetTargetApp = onSetTargetApp, onAddConfiguredStep = onAddConfiguredStep, onUpdateStep = onUpdateStep, onDeleteStep = onDeleteStep)
                    LfScreen.Run -> LiveRunScreen(document, runState, onCancelRun, onPauseRun, onResumeRun)
                    LfScreen.RunSuccess -> RunResultScreen(document, runState, success = true, onRerun = { lastRunTaskId?.let { id -> document.tasks.firstOrNull { it.id == id }?.let(runTaskAndShow) } }, onDetails = { screen = LfScreen.RunDetail }, onBack = { screen = LfScreen.Tasks })
                    LfScreen.RunFailure -> RunResultScreen(document, runState, success = false, onRerun = { lastRunTaskId?.let { id -> document.tasks.firstOrNull { it.id == id }?.let(runTaskAndShow) } }, onDetails = { screen = LfScreen.RunDetail }, onBack = { screen = LfScreen.Tasks })
                    LfScreen.RunDetail -> RunRecordDetailScreen(document, runRecords, stepRecords, selectedRunRecordId, onBack = { screen = LfScreen.RunSuccess }, onRerun = { lastRunTaskId?.let { id -> document.tasks.firstOrNull { it.id == id }?.let(runTaskAndShow) } })
                    LfScreen.History -> HistoryScreen(document, runRecords) { record -> selectedRunRecordId = record.id; onLoadRunDetail(record.id); screen = LfScreen.RunDetail }
                    LfScreen.Apps -> AppsScreen(installedApps, document, onToggleAppFavorite) { selectedTaskId = it; screen = LfScreen.TaskDetail }
                    LfScreen.Settings -> SettingsScreen(settings, numericSettings, onSettingChanged, onNumericSettingChanged, onClearHistory, onExportJson, onOpenOverlaySettings)
                    LfScreen.Permissions -> PermissionScreen(accessibilityGranted, overlayGranted, screenCaptureGranted, onOpenAccessibilitySettings, onOpenOverlaySettings, onStartCapture)
                    LfScreen.Import -> ImportScreen(importMessage, onExportJson, onImportJson)
                    LfScreen.More -> Unit
                    }
                }
                BottomNavigationBar(screen) { screen = it }
            }
        }
    }
}

private enum class LfScreen(val label: String, val symbol: String) {
    History("历史", "◷"),
    Apps("应用", "▦"),
    Settings("设置", "⚙"),
    TaskDetail("详情", "D"),
    Wizard("向导", "W"),
    RunSuccess("成功", "✓"),
    RunFailure("失败", "!"),
    RunDetail("运行详情", "L"),
    Home("总览", "✦"),
    Tasks("任务", "☰"),
    Editor("编排", "+"),
    Run("运行", "▶"),
    Permissions("权限", "✓"),
    Import("导入", "⇩"),
    More("更多", "⋯")
}

@Composable
private fun HomeScreen(
    document: AutomationDocument,
    runState: RunState,
    loading: Boolean,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onCancelRun: () -> Unit
) {
    val tasks = document.tasks
    val enabled = tasks.count { it.enabled }
    val totalSteps = tasks.sumOf { it.steps.size }
    val completion = if (tasks.isEmpty()) 0f else enabled.toFloat() / tasks.size.toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { AppHeader(chip = "Bento 2.0") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("下午好，文流用户", color = LfInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${tasks.size} 个任务 · $enabled 个已启用 · $totalSteps 个步骤",
                        color = LfMuted,
                        fontSize = 12.sp
                    )
                }
                StatusChip("● 服务已激活", LfPrimarySoft, LfPrimary)
            }
        }
        item { OverviewCard(completion, enabled, tasks.size, loading) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryActionButton("开始捕获", modifier = Modifier.weight(1f), onClick = onStartCapture)
                DangerActionButton("停止捕获", modifier = Modifier.weight(1f), onClick = onStopCapture)
            }
        }
        item {
            val runnableTask = tasks.firstOrNull { it.enabled } ?: tasks.firstOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryActionButton(
                    text = if (runState.status == RunStatus.RUNNING) "运行中" else "运行首个任务",
                    modifier = Modifier.weight(1f),
                    onClick = { runnableTask?.let(onRunTask) }
                )
                DangerActionButton("取消任务", modifier = Modifier.weight(1f), onClick = onCancelRun)
            }
        }
        item {
            Text("最近任务", color = LfMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        if (tasks.isEmpty()) {
            item {
                EmptyCard("暂无新版任务", "导入旧版 AutoInfo JSON，或保留旧库数据后重启进入自动转换。")
            }
        } else {
            items(tasks.take(3)) { task -> TaskCard(task) }
        }
    }
}

@Composable
private fun TasksScreen(
    document: AutomationDocument,
    selectedTaskId: Long?,
    onCreateTask: () -> Unit,
    onDeleteTask: (TaskModel) -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onToggleTaskEnabled: (TaskModel, Boolean) -> Unit,
    onToggleTaskFavorite: (TaskModel) -> Unit,
    onDuplicateTask: (TaskModel) -> Unit,
    onBatchExport: (List<TaskModel>) -> Unit,
    onSelect: (Long) -> Unit
) {
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var sortMode by remember { mutableStateOf("recent") }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    val filteredTasks = document.tasks
        .filter { task ->
            val text = query.trim()
            val matchesQuery = text.isBlank() || task.title.contains(text, true) || task.targetAppName.contains(text, true) || task.targetPackageName.contains(text, true) || task.steps.any { step -> step.title.contains(text, true) || step.actions.any { action -> action.text?.contains(text, true) == true } }
            val matchesCategory = when (category) {
                "game" -> task.category == "game"
                "ad" -> task.category == "ad"
                "favorite" -> task.favorite
                "enabled" -> task.enabled
                "disabled" -> !task.enabled
                else -> true
            }
            matchesQuery && matchesCategory
        }
        .sortedWith(Comparator { a, b ->
            if (a.favorite != b.favorite) return@Comparator if (a.favorite) -1 else 1
            when (sortMode) {
                "name" -> a.title.compareTo(b.title, ignoreCase = true)
                "duration" -> a.estimatedDurationMs.compareTo(b.estimatedDurationMs)
                else -> b.updatedAt.compareTo(a.updatedAt)
            }
        })
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    PageTitle("任务管理", "按 App Profile 管理自动化任务")
                }
                PrimaryActionButton("新建任务", modifier = Modifier.width(108.dp), onClick = onCreateTask)
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("搜索任务、应用或动作") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                listOf("all" to "全部", "game" to "手游", "ad" to "广告", "favorite" to "收藏", "enabled" to "已启用", "disabled" to "已停用").forEach { (value, label) ->
                    TextButton(onClick = { category = value }, modifier = Modifier.weight(1f)) { Text(label, color = if (category == value) LfPrimary else LfMuted, fontSize = 10.sp) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text("排序", color = LfMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                listOf("recent" to "最近", "name" to "名称", "duration" to "耗时").forEach { (value, label) ->
                    TextButton(onClick = { sortMode = value }) { Text(label, color = if (sortMode == value) LfPrimary else LfMuted, fontSize = 10.sp) }
                }
            }
        }
        if (selectedIds.isNotEmpty()) {
            item {
                CardBlock(color = LfPrimarySoft) {
                    Text("已选择 ${selectedIds.size} 个任务", color = LfInk, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { document.tasks.filter { it.id in selectedIds }.forEach { onToggleTaskEnabled(it, true) }; selectedIds = emptySet() }) { Text("启用") }
                        TextButton(onClick = { document.tasks.filter { it.id in selectedIds }.forEach { onToggleTaskEnabled(it, false) }; selectedIds = emptySet() }) { Text("停用") }
                        TextButton(onClick = { document.tasks.filter { it.id in selectedIds }.forEach(onDuplicateTask); selectedIds = emptySet() }) { Text("复制") }
                        TextButton(onClick = { onBatchExport(document.tasks.filter { it.id in selectedIds }) }) { Text("导出", color = LfPrimary) }
                        TextButton(onClick = { confirmBatchDelete = true }) { Text("删除", color = LfDanger) }
                    }
                    if (confirmBatchDelete) {
                        Text("删除后步骤配置无法恢复，确认删除这 ${selectedIds.size} 个任务？", color = LfDanger, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { confirmBatchDelete = false }) { Text("取消") }
                            TextButton(onClick = { document.tasks.filter { it.id in selectedIds }.forEach(onDeleteTask); selectedIds = emptySet(); confirmBatchDelete = false }) { Text("确认删除", color = LfDanger) }
                        }
                    }
                }
            }
        }
        if (filteredTasks.isEmpty()) {
            item { EmptyCard("没有任务", "新版数据库为空，导入旧版数据后会显示转换结果。") }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    selected = task.id == selectedTaskId,
                    onDelete = { onDeleteTask(task) },
                    onRun = { onRunTask(task) },
                    onToggleEnabled = { onToggleTaskEnabled(task, it) },
                    onToggleFavorite = { onToggleTaskFavorite(task) },
                    onDuplicate = { onDuplicateTask(task) },
                    onSelectToggle = { selectedIds = if (task.id in selectedIds) selectedIds - task.id else selectedIds + task.id },
                    onClick = { onSelect(task.id) }
                )
            }
        }
    }
}

@Composable
private fun EditorScreen(
    task: TaskModel?,
    onRunTask: (TaskModel) -> Unit,
    onAddWaitStep: (TaskModel) -> Unit,
    onRenameTask: (TaskModel, String) -> Unit,
    onDeleteStep: (TaskModel, StepModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("任务编排", task?.title ?: "选择任务后查看步骤") }
        if (task == null) {
            item { EmptyCard("未选择任务", "任务导入或创建后，这里会显示步骤、动作和感知条件。") }
        } else {
            item { TaskSummaryCard(task, onRunTask, onAddWaitStep, onRenameTask) }
            items(task.steps) { step -> StepCard(step) { onDeleteStep(task, step) } }
        }
    }
}

@Composable
private fun TaskDetailScreen(
    task: TaskModel,
    records: List<RunRecordEntity2>,
    onBack: () -> Unit,
    onRun: (TaskModel) -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSelectRecord: (Long) -> Unit
) {
    val taskRecords = records.filter { it.taskId == task.id }.take(5)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("返回", color = LfMuted) }
                Text("任务详情", color = LfInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = LfInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(task.targetAppName.ifBlank { task.targetPackageName.ifBlank { "未绑定应用" } }, color = LfMuted, fontSize = 12.sp)
                        Text("${task.scheduleType.label()} · ${task.steps.size} 步 · ${if (task.enabled) "已启用" else "已停用"}", color = LfMuted, fontSize = 11.sp)
                    }
                    Text(if (task.favorite) "★" else "☆", color = Color(0xFFE1A400), fontSize = 28.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryActionButton("立即运行", Modifier.weight(1f)) { onRun(task) }
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(48.dp)) { Text("编辑") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onToggle) { Text(if (task.enabled) "停用" else "启用", color = LfPrimary) }
                    TextButton(onClick = onDuplicate) { Text("复制", color = LfMuted) }
                    TextButton(onClick = onDelete) { Text("删除", color = LfDanger) }
                }
            }
        }
        item { Text("步骤编排", color = LfMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        if (task.steps.isEmpty()) item { EmptyCard("还没有步骤", "进入编辑向导添加文字识别、坐标、手势或等待动作。") }
        items(task.steps, key = { it.id }) { step -> StepCard(step) {} }
        item { Text("最近运行", color = LfMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        if (taskRecords.isEmpty()) item { EmptyCard("暂无运行记录", "运行任务后会在这里显示结果。") }
        items(taskRecords, key = { it.id }) { record ->
            CardBlock {
                Text(record.status, color = if (record.status == "COMPLETED") LfPrimary else LfDanger, fontWeight = FontWeight.Bold)
                Text("开始时间：${record.startedAt} · 结束时间：${record.endedAt ?: "运行中"}", color = LfMuted, fontSize = 11.sp)
                record.message?.let { Text(it, color = LfMuted, fontSize = 11.sp) }
                TextButton(onClick = { onSelectRecord(record.id) }) { Text("查看时间线", color = LfPrimary, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun TaskWizardScreen(
    task: TaskModel?,
    allTasks: List<TaskModel>,
    installedApps: List<InstalledAppOption>,
    ocrResult: OcrResultModel?,
    onStartCapture: () -> Unit,
    onRunOcrDebug: () -> Unit,
    onBack: () -> Unit,
    onSave: (TaskModel) -> Unit,
    onSetTargetApp: (TaskModel, String, String) -> Unit,
    onAddConfiguredStep: (TaskModel, StepModel) -> Unit,
    onUpdateStep: (TaskModel, StepModel) -> Unit,
    onDeleteStep: (TaskModel, StepModel) -> Unit
) {
    if (task == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageTitle("创建任务", "正在加载新任务")
            TextButton(onClick = onBack) { Text("返回") }
        }
        return
    }
    var title by remember(task.id, task.title) { mutableStateOf(task.title) }
    var query by remember(task.id, task.targetPackageName) { mutableStateOf(task.targetPackageName) }
    var schedule by remember(task.id, task.scheduleType) { mutableStateOf(task.scheduleType) }
    var autoOpen by remember(task.id, task.allowAutoOpenApp) { mutableStateOf(task.allowAutoOpenApp) }
    var category by remember(task.id, task.category) { mutableStateOf(task.category) }
    var startTime by remember(task.id, task.startTime) { mutableStateOf(task.startTime.orEmpty()) }
    var endTime by remember(task.id, task.endTime) { mutableStateOf(task.endTime.orEmpty()) }
    var runTimes by remember(task.id, task.runTimes) { mutableStateOf(task.runTimes.toString()) }
    var intervalMs by remember(task.id, task.intervalMs) { mutableStateOf(task.intervalMs.toString()) }
    var editorType by remember(task.id) { mutableStateOf<ActionType?>(null) }
    var editingStep by remember(task.id) { mutableStateOf<StepModel?>(null) }
    val filteredApps = installedApps.filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }.take(8)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("取消", color = LfMuted) }
            Column(Modifier.weight(1f)) { Text("创建 / 编辑任务", color = LfInk, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("基础信息 → 目标应用 → 调度 → 步骤", color = LfMuted, fontSize = 11.sp) }
            TextButton(onClick = { onSave(task.copy(title = title.trim().ifBlank { "未命名任务" }, scheduleType = schedule, allowAutoOpenApp = autoOpen, category = category, startTime = startTime.trim().ifBlank { null }, endTime = endTime.trim().ifBlank { null }, runTimes = runTimes.toIntOrNull()?.coerceAtLeast(1) ?: 1, intervalMs = intervalMs.toLongOrNull()?.coerceAtLeast(0L) ?: 0L)) }) { Text("保存", color = LfPrimary, fontWeight = FontWeight.Bold) }
        }
        DetailSection("基础信息") {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("任务名称") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            DetailChoiceRow("调度类型", listOf("手动", "每日", "每周", "循环"), listOf(ScheduleType.MANUAL, ScheduleType.DAILY, ScheduleType.WEEKLY, ScheduleType.LOOP), schedule) { schedule = it }
            Spacer(Modifier.height(8.dp))
            DetailChoiceRow("自动打开应用", listOf("关闭", "开启"), listOf(false, true), autoOpen) { autoOpen = it }
            Spacer(Modifier.height(8.dp))
            DetailChoiceRow("任务分类", listOf("通用", "手游", "广告"), listOf("general", "game", "ad"), category.ifBlank { "general" }) { category = it }
            if (schedule == ScheduleType.DAILY || schedule == ScheduleType.WEEKLY) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(startTime, { startTime = it }, Modifier.weight(1f), label = { Text("开始 HH:mm") }, singleLine = true)
                    OutlinedTextField(endTime, { endTime = it }, Modifier.weight(1f), label = { Text("结束 HH:mm") }, singleLine = true)
                }
            }
            if (schedule == ScheduleType.LOOP || schedule == ScheduleType.RUN_COUNT) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(runTimes, { runTimes = it }, Modifier.weight(1f), label = { Text("运行次数") }, singleLine = true)
                    OutlinedTextField(intervalMs, { intervalMs = it }, Modifier.weight(1f), label = { Text("间隔 ms") }, singleLine = true)
                }
            }
        }
        DetailSection("目标应用") {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("应用名称或包名") }, singleLine = true)
            filteredApps.forEach { app ->
                DetailActionRow(app.label, app.packageName) { onSetTargetApp(task, app.label, app.packageName); query = app.packageName }
            }
            Text("当前：${task.targetAppName.ifBlank { "未选择" }}", color = LfMuted, fontSize = 11.sp)
        }
        DetailSection("添加步骤") {
            val choices = listOf(
                ActionType.TAP_TEXT to "点击文字",
                ActionType.WAIT_FOR_TEXT to "等待文字",
                ActionType.WAIT_FOR_SCREEN to "等待页面",
                ActionType.TAP_COORDINATE to "点击坐标",
                ActionType.LONG_PRESS to "长按坐标",
                ActionType.SWIPE to "滑动手势",
                ActionType.INPUT_TEXT to "输入文字",
                ActionType.WAIT to "固定等待",
                ActionType.BACK to "返回键",
                ActionType.OPEN_APP to "打开应用",
                ActionType.RUN_SUBTASK to "执行子任务"
            )
            choices.forEach { (type, label) ->
                DetailActionRow(label, "先配置参数，再保存为真实步骤") { editingStep = null; editorType = type }
            }
        }
        if (editorType != null) {
            StepConfigEditor(
                task = task,
                initial = editingStep,
                actionType = editorType!!,
                installedApps = installedApps,
                allTasks = allTasks,
                ocrResult = ocrResult,
                onStartCapture = onStartCapture,
                onRunOcrDebug = onRunOcrDebug,
                onCancel = { editorType = null; editingStep = null },
                onSave = { step ->
                    if (editingStep == null) onAddConfiguredStep(task, step) else onUpdateStep(task, step)
                    editorType = null
                    editingStep = null
                }
            )
        }
        Text("当前步骤 ${task.steps.size} 个", color = LfMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        task.steps.forEach { step ->
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("${step.orderIndex + 1}. ${step.title}", color = LfInk, fontWeight = FontWeight.Bold)
                        Text(stepSummary(step), color = LfMuted, fontSize = 11.sp)
                    }
                    TextButton(onClick = { editingStep = step; editorType = step.actionType() }) { Text("编辑", color = LfPrimary, fontSize = 11.sp) }
                    TextButton(onClick = { onDeleteStep(task, step) }) { Text("删除", color = LfDanger, fontSize = 11.sp) }
                }
            }
        }
    }
}

private fun StepModel.actionType(): ActionType = actions.firstOrNull()?.type
    ?: if (conditions.firstOrNull()?.type == ConditionType.SCREEN_STATE) ActionType.WAIT_FOR_SCREEN else ActionType.WAIT_FOR_TEXT

private fun stepSummary(step: StepModel): String {
    val action = step.actions.firstOrNull()
    val condition = step.conditions.firstOrNull()
    return when {
        action?.text?.isNotBlank() == true -> "${action.type.label()}：${action.text}"
        action?.inputText?.isNotBlank() == true -> "输入：${action.inputText}"
        action?.targetPackageName?.isNotBlank() == true -> "打开：${action.targetAppName ?: action.targetPackageName}"
        action?.swipe != null -> "${action.type.label()} · ${action.swipe.durationMs}ms"
        condition?.text?.isNotBlank() == true -> "页面必须出现：${condition.text}"
        else -> step.actions.firstOrNull()?.type?.label() ?: "条件 / 等待"
    }
}

@Composable
private fun StepConfigEditor(
    task: TaskModel,
    initial: StepModel?,
    actionType: ActionType,
    installedApps: List<InstalledAppOption>,
    allTasks: List<TaskModel>,
    ocrResult: OcrResultModel?,
    onStartCapture: () -> Unit,
    onRunOcrDebug: () -> Unit,
    onCancel: () -> Unit,
    onSave: (StepModel) -> Unit
) {
    val initialAction = initial?.actions?.firstOrNull()
    val initialCondition = initial?.conditions?.firstOrNull()
    var title by remember(actionType, initial?.id) { mutableStateOf(initial?.title ?: actionType.label()) }
    var text by remember(actionType, initial?.id) { mutableStateOf(initialAction?.text ?: initialCondition?.text ?: "") }
    var excludedText by remember(actionType, initial?.id) { mutableStateOf(initialAction?.excludedText ?: initialCondition?.excludedText ?: "") }
    var inputText by remember(actionType, initial?.id) { mutableStateOf(initialAction?.inputText ?: "") }
    var matchType by remember(actionType, initial?.id) { mutableStateOf(initialAction?.matchType ?: initialCondition?.matchType ?: MatchType.FUZZY) }
    var source by remember(actionType, initial?.id) { mutableStateOf(initialAction?.recognitionSource ?: initialCondition?.recognitionSource ?: RecognitionSource.AUTO) }
    var x by remember(actionType, initial?.id) { mutableStateOf(initialAction?.x ?: 0.5f) }
    var y by remember(actionType, initial?.id) { mutableStateOf(initialAction?.y ?: 0.5f) }
    var repeat by remember(actionType, initial?.id) { mutableStateOf((initialAction?.repeatCount ?: 1).toString()) }
    var timeout by remember(actionType, initial?.id) { mutableStateOf((initial?.timeoutMs ?: initialAction?.timeoutMs ?: initialCondition?.timeoutMs ?: 5000L).toString()) }
    var delayAfter by remember(actionType, initial?.id) { mutableStateOf((initial?.delayAfterMs ?: 0L).toString()) }
    var failurePolicy by remember(actionType, initial?.id) { mutableStateOf(initial?.failurePolicy ?: FailurePolicy.STOP) }
    var swipeDirection by remember(actionType, initial?.id) { mutableStateOf("up") }
    var swipeDuration by remember(actionType, initial?.id) { mutableStateOf((initialAction?.swipe?.durationMs ?: 400L).toString()) }
    var pressDuration by remember(actionType, initial?.id) { mutableStateOf((initialAction?.timeoutMs ?: 800L).toString()) }
    var targetPackage by remember(actionType, initial?.id) { mutableStateOf(initialAction?.targetPackageName ?: task.targetPackageName) }
    var targetApp by remember(actionType, initial?.id) { mutableStateOf(initialAction?.targetAppName ?: task.targetAppName) }
    var childId by remember(actionType, initial?.id) { mutableStateOf(initialAction?.childTaskId) }
    var validationError by remember(actionType, initial?.id) { mutableStateOf<String?>(null) }
    val timeoutMs = timeout.toLongOrNull()?.coerceAtLeast(0L) ?: 5000L
    val roi = initialAction?.roi
    val selectedBlocks = ocrResult?.blocks.orEmpty().take(20)

    DetailSection("配置：${actionType.label()}") {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("步骤名称") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        when (actionType) {
            ActionType.TAP_TEXT, ActionType.WAIT_FOR_TEXT, ActionType.WAIT_FOR_SCREEN -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("目标文字", color = LfInk, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onStartCapture) { Text("开始捕获", color = LfPrimary, fontSize = 11.sp) }
                    TextButton(onClick = onRunOcrDebug) { Text("运行 OCR", color = LfPrimary, fontSize = 11.sp) }
                }
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(if (actionType == ActionType.WAIT_FOR_SCREEN) "必须出现的词（用 # 分隔）" else "目标文字") }, singleLine = true)
                if (actionType == ActionType.WAIT_FOR_SCREEN) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(excludedText, { excludedText = it }, Modifier.fillMaxWidth(), label = { Text("不能出现的词（用 # 分隔）") }, singleLine = true)
                }
                if (selectedBlocks.isNotEmpty()) {
                    Text("点击识别结果填入目标文字", color = LfMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedBlocks.forEach { block -> TextButton(onClick = { text = block.text }) { Text(block.text, color = LfPrimary, fontSize = 11.sp) } }
                    }
                }
                DetailChoiceRow("匹配方式", listOf("精确", "模糊", "多关键词"), listOf(MatchType.EXACT, MatchType.FUZZY, MatchType.ANY_KEYWORD), matchType) { matchType = it }
                DetailChoiceRow("识别源", listOf("自动", "无障碍", "OCR"), listOf(RecognitionSource.AUTO, RecognitionSource.ACCESSIBILITY, RecognitionSource.OCR), source) { source = it }
            }
            ActionType.TAP_COORDINATE, ActionType.LONG_PRESS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberField("X (0~1)", x.toString(), Modifier.weight(1f)) { x = it.toFloatOrNull()?.coerceIn(0f, 1f) ?: x }
                    NumberField("Y (0~1)", y.toString(), Modifier.weight(1f)) { y = it.toFloatOrNull()?.coerceIn(0f, 1f) ?: y }
                }
                if (actionType == ActionType.LONG_PRESS) NumberField("长按时长 ms", pressDuration, Modifier.fillMaxWidth()) { pressDuration = it }
                NumberField("重复次数", repeat, Modifier.fillMaxWidth()) { repeat = it }
            }
            ActionType.SWIPE -> {
                DetailChoiceRow("方向", listOf("上滑", "下滑", "左滑", "右滑"), listOf("up", "down", "left", "right"), swipeDirection) { swipeDirection = it }
                NumberField("持续时间 ms", swipeDuration, Modifier.fillMaxWidth()) { swipeDuration = it }
            }
            ActionType.INPUT_TEXT -> OutlinedTextField(inputText, { inputText = it }, Modifier.fillMaxWidth().height(120.dp), label = { Text("要输入的文本") })
            ActionType.WAIT -> NumberField("等待时长 ms", timeout, Modifier.fillMaxWidth()) { timeout = it }
            ActionType.BACK -> Text("执行一次系统返回键。", color = LfMuted, fontSize = 12.sp)
            ActionType.OPEN_APP -> {
                OutlinedTextField(targetPackage, { targetPackage = it }, Modifier.fillMaxWidth(), label = { Text("包名") }, singleLine = true)
                installedApps.take(12).forEach { app -> DetailActionRow(app.label, app.packageName) { targetPackage = app.packageName; targetApp = app.label } }
            }
            ActionType.RUN_SUBTASK -> {
                Text("选择要串行执行的子任务", color = LfMuted, fontSize = 11.sp)
                allTasks.filter { it.id != task.id }.forEach { child -> DetailActionRow(child.title, child.targetAppName) { childId = child.id } }
                Text("当前：${allTasks.firstOrNull { it.id == childId }?.title ?: "未选择"}", color = LfInk, fontSize = 12.sp)
            }
        }
        if (actionType == ActionType.TAP_TEXT || actionType == ActionType.WAIT_FOR_TEXT || actionType == ActionType.WAIT_FOR_SCREEN) NumberField("超时 ms", timeout, Modifier.fillMaxWidth()) { timeout = it }
        Spacer(Modifier.height(8.dp))
        NumberField("执行后等待 ms", delayAfter, Modifier.fillMaxWidth()) { delayAfter = it }
        DetailChoiceRow("失败策略", listOf("停止", "重试", "跳过"), listOf(FailurePolicy.STOP, FailurePolicy.RETRY, FailurePolicy.SKIP), failurePolicy) { failurePolicy = it }
        validationError?.let { Text(it, color = LfDanger, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(48.dp)) { Text("取消") }
            PrimaryActionButton("保存步骤", Modifier.weight(1f)) {
                val invalid = when {
                    (actionType == ActionType.TAP_TEXT || actionType == ActionType.WAIT_FOR_TEXT || actionType == ActionType.WAIT_FOR_SCREEN) && text.trim().isBlank() -> "请填写目标文字或先从 OCR 结果中选择文字。"
                    actionType == ActionType.INPUT_TEXT && inputText.isBlank() -> "请输入要填入的文本。"
                    actionType == ActionType.OPEN_APP && targetPackage.trim().isBlank() -> "请选择或填写目标应用包名。"
                    actionType == ActionType.RUN_SUBTASK && childId == null -> "请选择一个子任务。"
                    else -> null
                }
                if (invalid != null) {
                    validationError = invalid
                } else {
                val action = when (actionType) {
                    ActionType.TAP_TEXT, ActionType.WAIT_FOR_TEXT -> ActionSpec(actionType, text = text.trim(), matchType = matchType, recognitionSource = source, timeoutMs = timeoutMs, repeatCount = repeat.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                    ActionType.WAIT_FOR_SCREEN -> ActionSpec(actionType, text = text.trim(), excludedText = excludedText.trim().ifBlank { null }, matchType = matchType, recognitionSource = source, timeoutMs = timeoutMs)
                    ActionType.TAP_COORDINATE -> ActionSpec(actionType, x = x, y = y, repeatCount = repeat.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                    ActionType.LONG_PRESS -> ActionSpec(actionType, x = x, y = y, timeoutMs = pressDuration.toLongOrNull()?.coerceAtLeast(500L) ?: 800L)
                    ActionType.SWIPE -> ActionSpec(actionType, swipe = directionSwipe(swipeDirection, swipeDuration.toLongOrNull()?.coerceAtLeast(1L) ?: 400L))
                    ActionType.INPUT_TEXT -> ActionSpec(actionType, inputText = inputText)
                    ActionType.WAIT -> ActionSpec(actionType, timeoutMs = timeoutMs)
                    ActionType.BACK -> ActionSpec(actionType)
                    ActionType.OPEN_APP -> ActionSpec(actionType, targetPackageName = targetPackage.trim(), targetAppName = targetApp.trim())
                    ActionType.RUN_SUBTASK -> ActionSpec(actionType, childTaskId = childId, childTaskTitle = allTasks.firstOrNull { it.id == childId }?.title)
                }
                val isPageCondition = actionType == ActionType.WAIT_FOR_SCREEN
                val condition = if (isPageCondition) ConditionSpec(ConditionType.SCREEN_STATE, text = text.trim(), excludedText = excludedText.trim().ifBlank { null }, matchType = matchType, recognitionSource = source, timeoutMs = timeoutMs) else null
                onSave(StepModel(initial?.id ?: 0L, task.id, initial?.orderIndex ?: task.steps.size, if (isPageCondition || actionType == ActionType.WAIT_FOR_TEXT) StepType.CONDITION else if (actionType == ActionType.RUN_SUBTASK) StepType.SUBTASK else if (actionType == ActionType.WAIT) StepType.WAIT else StepType.ACTION, title.trim().ifBlank { actionType.label() }, timeoutMs, delayAfter.toLongOrNull()?.coerceAtLeast(0L) ?: 0L, failurePolicy, if (condition == null) listOf(action) else emptyList(), if (condition == null) emptyList() else listOf(condition)))
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, modifier, label = { Text(label) }, singleLine = true)
}

private fun directionSwipe(direction: String, duration: Long): SwipeSpec = when (direction) {
    "down" -> SwipeSpec(0.5f, 0.22f, 0.5f, 0.78f, duration)
    "left" -> SwipeSpec(0.78f, 0.5f, 0.22f, 0.5f, duration)
    "right" -> SwipeSpec(0.22f, 0.5f, 0.78f, 0.5f, duration)
    else -> SwipeSpec(0.5f, 0.78f, 0.5f, 0.22f, duration)
}

@Composable
private fun RunResultScreen(
    document: AutomationDocument,
    runState: RunState,
    success: Boolean,
    onRerun: () -> Unit,
    onDetails: () -> Unit,
    onBack: () -> Unit
) {
    val task = document.tasks.firstOrNull { it.id == runState.taskId }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(30.dp))
        Text(if (success) "✓" else "!", color = if (success) LfPrimary else LfDanger, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        Text(if (success) "自动化任务执行完成" else "任务未完全完成", color = LfInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(task?.title ?: runState.taskTitle ?: "未知任务", color = LfMuted, fontSize = 13.sp)
        CardBlock(color = if (success) LfPrimarySoft else LfDangerSoft) {
            Text(if (success) "所有步骤已按顺序执行" else "${runState.message ?: "执行中断，请检查权限或识别条件"}", color = LfInk, fontSize = 13.sp)
            Text("完成 ${runState.currentStepIndex} / ${runState.totalSteps} 步", color = LfMuted, fontSize = 11.sp)
        }
        PrimaryActionButton(if (success) "再次运行该任务" else "重新运行该任务", Modifier.fillMaxWidth(), onRerun)
        OutlinedButton(onClick = onDetails, Modifier.fillMaxWidth().height(48.dp)) { Text("查看运行详情与日志") }
        TextButton(onClick = onBack) { Text("返回任务主页", color = LfMuted) }
    }
}

@Composable
private fun RunRecordDetailScreen(
    document: AutomationDocument,
    records: List<RunRecordEntity2>,
    stepRecords: List<com.monster.literaryflow.data.db2.StepRecordEntity2>,
    recordId: Long?,
    onBack: () -> Unit,
    onRerun: () -> Unit
) {
    val record = records.firstOrNull { it.id == recordId }
    val task = document.tasks.firstOrNull { it.id == record?.taskId }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("返回") }; Text("执行记录", color = LfInk, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        CardBlock {
            Text(task?.title ?: "任务 #${record?.taskId ?: "-"}", color = LfInk, fontWeight = FontWeight.Bold)
            Text("状态：${record?.status ?: "暂无记录"}", color = if (record?.status == "COMPLETED") LfPrimary else LfDanger, fontSize = 13.sp)
            Text("开始：${record?.startedAt ?: "-"}", color = LfMuted, fontSize = 11.sp)
            Text("结束：${record?.endedAt ?: "-"}", color = LfMuted, fontSize = 11.sp)
            record?.message?.let { Text(it, color = LfMuted, fontSize = 11.sp) }
        }
        Text("执行时间线", color = LfMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        task?.steps?.forEachIndexed { index, step ->
            val stepRecord = stepRecords.firstOrNull { it.stepId == step.id }
            CardBlock {
                Text("第 ${index + 1} 步 · ${step.title}", color = LfInk, fontWeight = FontWeight.Bold)
                Text(stepRecord?.status ?: "未执行", color = if (stepRecord?.status == "COMPLETED") LfPrimary else LfMuted, fontSize = 11.sp)
                Text(step.actions.joinToString { it.type.label() }.ifBlank { "条件 / 等待" }, color = LfMuted, fontSize = 11.sp)
            }
        }
        PrimaryActionButton("重新运行此任务", Modifier.fillMaxWidth(), onRerun)
    }
}

@Composable
private fun HistoryScreen(
    document: AutomationDocument,
    records: List<RunRecordEntity2>,
    onSelectRecord: (RunRecordEntity2) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    val visibleRecords = records.filter { record ->
        val task = document.tasks.firstOrNull { it.id == record.taskId }
        (query.isBlank() || task?.title?.contains(query, true) == true || task?.targetAppName?.contains(query, true) == true || record.message?.contains(query, true) == true) &&
            (statusFilter == "ALL" || record.status == statusFilter)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("运行历史", "每次启动、完成、失败和取消都会保留在新版 RunRecord") }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("搜索任务、应用或失败原因") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("ALL" to "全部", "COMPLETED" to "成功", "FAILED" to "失败", "CANCELLED" to "已取消").forEach { (value, label) ->
                    TextButton(onClick = { statusFilter = value }, modifier = Modifier.weight(1f)) { Text(label, color = if (statusFilter == value) LfPrimary else LfMuted, fontSize = 10.sp) }
                }
            }
        }
        if (visibleRecords.isEmpty()) item { EmptyCard("暂无相关运行记录", "启动一个有步骤的任务后，结果会显示在这里。") }
        items(visibleRecords, key = { it.id }) { record ->
            val task = document.tasks.firstOrNull { it.id == record.taskId }
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task?.title ?: "任务 #${record.taskId}", color = LfInk, fontWeight = FontWeight.Bold)
                        Text("开始 ${record.startedAt}  ·  ${record.status}", color = LfMuted, fontSize = 11.sp)
                        record.message?.takeIf { it.isNotBlank() }?.let { Text(it, color = LfMuted, fontSize = 11.sp) }
                    }
                    TextButton(onClick = { onSelectRecord(record) }) { Text("查看详情", color = LfPrimary) }
                }
            }
        }
    }
}

@Composable
private fun AppsScreen(installedApps: List<InstalledAppOption>, document: AutomationDocument, onToggleAppFavorite: (String) -> Unit, onSelectTask: (Long) -> Unit) {
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { PageTitle("应用管理", "从系统 PackageManager 读取可启动应用") }
        if (installedApps.isEmpty()) item { EmptyCard("没有可用应用", "系统当前没有返回可启动的应用。") }
        items(installedApps, key = { it.packageName }) { app ->
            val linked = document.tasks.filter { it.targetPackageName == app.packageName }
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, color = LfInk, fontWeight = FontWeight.Bold)
                        Text(app.packageName, color = LfMuted, fontSize = 11.sp)
                    }
                    TextButton(onClick = { selectedPackage = if (selectedPackage == app.packageName) null else app.packageName }) { Text("详情", color = LfPrimary) }
                    TextButton(onClick = { onToggleAppFavorite(app.packageName) }) { Text(if (document.appProfiles.firstOrNull { it.packageName == app.packageName }?.favorite == true) "★" else "☆", color = Color(0xFFE1A400)) }
                }
                if (selectedPackage == app.packageName) {
                    Spacer(Modifier.height(8.dp))
                    Text("已绑定任务 ${linked.size} 个", color = LfMuted, fontSize = 11.sp)
                    linked.forEach { task ->
                        TextButton(onClick = { onSelectTask(task.id) }, modifier = Modifier.fillMaxWidth()) { Text(task.title, color = LfInk, fontSize = 12.sp) }
                    }
                    if (linked.isEmpty()) Text("暂无绑定任务，可在任务向导中选择此应用。", color = LfMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(settings: Map<String, Boolean>, numericSettings: Map<String, Int>, onSettingChanged: (String, Boolean) -> Unit, onNumericSettingChanged: (String, Int) -> Unit, onClearHistory: () -> Unit, onExportJson: () -> Unit, onOpenOverlaySettings: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageTitle("系统设置", "运行策略、权限和数据资源")
        CardBlock {
            Text("数据与备份", color = LfInk, fontWeight = FontWeight.Bold)
            Text("导出当前新版 schema JSON，用于迁移和诊断。", color = LfMuted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton("导出任务 JSON", Modifier.fillMaxWidth(), onExportJson)
        }
        CardBlock {
            Text("默认运行策略", color = LfInk, fontWeight = FontWeight.Bold)
            SettingToggle("默认自动打开目标应用", settings["autoOpenApp"] == true) { onSettingChanged("autoOpenApp", it) }
            SettingToggle("失败时自动重试", settings["retryOnFail"] == true) { onSettingChanged("retryOnFail", it) }
            SettingToggle("显示悬浮控制器", settings["floatingController"] == true) { onSettingChanged("floatingController", it) }
            SettingToggle("完成时震动反馈", settings["vibration"] == true) { onSettingChanged("vibration", it) }
            Spacer(Modifier.height(8.dp))
            Text("OCR 置信度阈值 ${numericSettings["ocrConfidence"] ?: 85}%", color = LfMuted, fontSize = 11.sp)
            androidx.compose.material3.Slider(
                value = (numericSettings["ocrConfidence"] ?: 85).toFloat(),
                onValueChange = { onNumericSettingChanged("ocrConfidence", it.toInt()) },
                valueRange = 50f..99f
            )
            Text("悬浮窗透明度 ${numericSettings["floatingOpacity"] ?: 85}%", color = LfMuted, fontSize = 11.sp)
            androidx.compose.material3.Slider(
                value = (numericSettings["floatingOpacity"] ?: 85).toFloat(),
                onValueChange = { onNumericSettingChanged("floatingOpacity", it.toInt()) },
                valueRange = 30f..100f
            )
        }
        CardBlock {
            Text("悬浮控制", color = LfInk, fontWeight = FontWeight.Bold)
            Text("允许运行期间显示暂停、继续和停止入口。", color = LfMuted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton("打开悬浮窗权限", Modifier.fillMaxWidth(), onOpenOverlaySettings)
        }
        CardBlock {
            Text("运行历史", color = LfInk, fontWeight = FontWeight.Bold)
            Text("清理本地 RunRecord 和步骤时间线。", color = LfMuted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            DangerActionButton("清空历史记录", Modifier.fillMaxWidth(), onClick = { confirmClear = true })
            if (confirmClear) {
                Text("确认清空所有运行历史？此操作不可恢复。", color = LfDanger, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { confirmClear = false }, modifier = Modifier.weight(1f)) { Text("取消") }
                    DangerActionButton("确认清空", Modifier.weight(1f), onClick = { onClearHistory(); confirmClear = false })
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LfInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { onCheckedChange(!checked) }) {
            Text(if (checked) "已开启" else "已关闭", color = if (checked) LfPrimary else LfMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LiveRunScreen(document: AutomationDocument, runState: RunState, onCancelRun: () -> Unit, onPauseRun: () -> Unit, onResumeRun: () -> Unit) {
    var showStopConfirm by remember { mutableStateOf(false) }
    val current = runState.taskId?.let { taskId -> document.tasks.firstOrNull { it.id == taskId } }
        ?: document.tasks.firstOrNull()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LfDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(14.dp))
        Text("实时运行", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("感知一次，缓存结果；变化后再重新识别", color = Color(0xFF9DB0C2), fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))
        DarkPanel {
            Text(runState.taskTitle ?: current?.title ?: "待机", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(runState.status.label(), color = LfPrimarySoft, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (runState.status == RunStatus.COMPLETED || runState.status == RunStatus.FAILED || runState.status == RunStatus.CANCELLED) {
                Spacer(Modifier.height(10.dp))
                Text(
                    when (runState.status) {
                        RunStatus.COMPLETED -> "任务已完成，运行记录已保存"
                        RunStatus.FAILED -> "任务失败，检查步骤和权限后可重试"
                        else -> "任务已取消，运行记录已保存"
                    },
                    color = if (runState.status == RunStatus.COMPLETED) LfPrimarySoft else Color(0xFFFFB4B4),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = if (runState.totalSteps == 0) 0f else runState.currentStepIndex.toFloat() / runState.totalSteps.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = LfPrimary,
                trackColor = Color(0xFF24384A)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkChip("OCR NORMAL")
                DarkChip(runState.perceptionMode.name)
                DarkChip("可取消")
            }
            runState.message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFB8C6D5), fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (runState.status == RunStatus.PAUSED) {
                    PrimaryActionButton("继续", Modifier.weight(1f), onResumeRun)
                } else {
                    PrimaryActionButton("暂停", Modifier.weight(1f), onPauseRun)
                }
                DangerActionButton("停止", modifier = Modifier.weight(1f), onClick = { showStopConfirm = true })
            }
            if (showStopConfirm) {
                Spacer(Modifier.height(8.dp))
                Text("停止后本次执行会标记为已取消，已完成步骤仍会保存。", color = Color(0xFFFFB4B4), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showStopConfirm = false }, modifier = Modifier.weight(1f)) { Text("继续运行", color = Color.White) }
                    DangerActionButton("停止并保存", Modifier.weight(1f), onClick = { showStopConfirm = false; onCancelRun() })
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        DarkPanel {
            Text("当前步骤", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            current?.steps?.take(4)?.forEachIndexed { index, step ->
                Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) LfPrimary else Color(0xFF24384A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(step.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(step.type.name, color = Color(0xFF9DB0C2), fontSize = 10.sp)
                    }
                }
            } ?: Text("没有运行中的任务", color = Color(0xFF9DB0C2), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PermissionScreen(
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    screenCaptureGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onStartCapture: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("权限中心", "启动自动化前的运行环境检查") }
        item {
            CardBlock(color = LfPrimary) {
                Text("✓", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("核心权限检查", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("无障碍、悬浮窗、屏幕捕获保持分离授权", color = LfPrimarySoft, fontSize = 11.sp)
            }
        }
        item { PermissionRow("无障碍服务", "识别页面并执行点击", "打开", onOpenAccessibilitySettings) }
        item { PermissionRow("悬浮窗", "显示运行状态与停止入口", "打开", onOpenOverlaySettings) }
        item { PermissionRow("屏幕捕获", "截图与文字识别", "授权", onStartCapture) }
        item {
            CardBlock(color = if (accessibilityGranted && overlayGranted && screenCaptureGranted) LfPrimarySoft else LfWarningSoft) {
                Text("当前权限状态", color = LfInk, fontWeight = FontWeight.Bold)
                Text("无障碍：${if (accessibilityGranted) "已开启" else "未开启"} · 悬浮窗：${if (overlayGranted) "已开启" else "未开启"} · 屏幕捕获：${if (screenCaptureGranted) "已授权" else "未授权"}", color = LfMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ImportScreen(
    importMessage: String?,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        PageTitle("导入任务", "支持新版 schema 和旧版 AutoInfo JSON")
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            label = { Text("粘贴 JSON") },
            placeholder = { Text("[{ title, runInfo, loopType ... }]") },
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryActionButton("导出当前任务", modifier = Modifier.weight(1f), onClick = onExportJson)
            PrimaryActionButton("转换并导入", modifier = Modifier.weight(1f)) {
                onImportJson(text)
            }
        }
        importMessage?.let {
            Spacer(Modifier.height(12.dp))
            CardBlock {
                Text(it, color = LfInk, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        CardBlock(color = LfWarningSoft) {
            Text("兼容说明", color = LfInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "旧版坐标会归一化，子任务会保留标题和语义；转换保证数据结构正常落入新版模型，但不承诺导入后无需重新校准即可运行。",
                color = LfMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AppHeader(chip: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LfPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("文流 2.0", color = LfInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Android 视觉感知自动化", color = LfMuted, fontSize = 11.sp)
        }
        StatusChip(chip, LfPrimarySoft, LfPrimary)
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = LfInk, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = LfMuted, fontSize = 11.sp)
    }
}

@Composable
private fun OverviewCard(completion: Float, enabled: Int, total: Int, loading: Boolean) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("自动化运行中", color = LfInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("达成率 ${(completion * 100).toInt()}%", color = LfMuted, fontSize = 12.sp)
            }
            StatusChip("达成率 ${(completion * 100).toInt()}%", LfPrimarySoft, LfPrimary)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = completion, loading = loading)
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricLine("已启用任务", "$enabled / $total")
                MetricLine("OCR 模式", "自适应")
                MetricLine("运行策略", "可取消状态机")
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, loading: Boolean) {
    Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(
                color = Color(0xFFE8F2EF),
                radius = size.minDimension / 2f - stroke.width / 2f,
                style = stroke
            )
            drawArc(
                color = LfPrimary,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(stroke.width / 2f, stroke.width / 2f),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke
            )
        }
        if (loading) {
            CircularProgressIndicator(color = LfPrimary, modifier = Modifier.size(40.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress * 100).toInt()}%", color = LfInk, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("任务", color = LfMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("●", color = LfPrimary, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = LfMuted, fontSize = 10.sp)
            Text(value, color = LfInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskModel,
    selected: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onRun: (() -> Unit)? = null,
    onToggleEnabled: ((Boolean) -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onSelectToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (selected) LfPrimary else Color.Transparent
    Card(
        onClick = { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = LfCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (task.enabled) LfPrimary else LfSkySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(if (task.enabled) "▶" else "Ⅱ", color = if (task.enabled) Color.White else LfMuted)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = LfInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(task.targetAppName.ifBlank { task.targetPackageName.ifBlank { "未绑定应用" } }, color = LfMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(task.scheduleType.label(), LfSkySoft, Color(0xFF149BDF))
                    StatusChip("${task.steps.size} 步", LfPrimarySoft, LfPrimary)
                }
            }
            onSelectToggle?.let {
                TextButton(onClick = it) { Text("选择", color = LfMuted, fontSize = 11.sp) }
            }
            onRun?.let {
                TextButton(onClick = it) {
                    Text("运行", color = LfPrimary, fontWeight = FontWeight.Bold)
                }
            }
            onToggleFavorite?.let {
                TextButton(onClick = it) { Text(if (task.favorite) "★" else "☆", color = Color(0xFFE1A400), fontSize = 18.sp) }
            }
            onToggleEnabled?.let {
                TextButton(onClick = { it(!task.enabled) }) { Text(if (task.enabled) "停用" else "启用", color = LfPrimary, fontSize = 11.sp) }
            }
            onDuplicate?.let {
                TextButton(onClick = it) { Text("复制", color = LfMuted, fontSize = 11.sp) }
            }
            onDelete?.let {
                TextButton(onClick = it) {
                    Text("删除", color = LfDanger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TaskSummaryCard(
    task: TaskModel,
    onRunTask: (TaskModel) -> Unit,
    onAddWaitStep: (TaskModel) -> Unit,
    onRenameTask: (TaskModel, String) -> Unit
) {
    var title by remember(task.id, task.title) { mutableStateOf(task.title) }
    CardBlock {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("任务名称") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("目标应用：${task.targetAppName.ifBlank { task.targetPackageName.ifBlank { "未绑定" } }}", color = LfMuted, fontSize = 12.sp)
        Text("调度：${task.scheduleType.label()} · ${task.runTimes} 次 · ${task.steps.size} 步", color = LfMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryActionButton("保存名称", modifier = Modifier.weight(1f)) {
                onRenameTask(task, title.trim().ifBlank { task.title })
            }
            PrimaryActionButton("添加等待步骤", modifier = Modifier.weight(1f)) {
                onAddWaitStep(task)
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryActionButton("运行此任务", modifier = Modifier.fillMaxWidth()) { onRunTask(task) }
    }
}

@Composable
private fun StepCard(step: StepModel, onDelete: () -> Unit) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LfPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text("${step.orderIndex + 1}", color = LfPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step.title, color = LfInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${step.type.name} · ${step.failurePolicy.label()}", color = LfMuted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Divider(color = Color(0xFFE9F0EE))
        Spacer(Modifier.height(10.dp))
        val actionNames = step.actions.joinToString("、") { it.type.label() }
        val conditionNames = step.conditions.joinToString("、") { it.type.name }
        Text(
            text = actionNames.ifBlank { conditionNames.ifBlank { "等待/空步骤" } },
            color = LfMuted,
            fontSize = 11.sp
        )
        TextButton(onClick = onDelete) {
            Text("删除步骤", color = LfDanger, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, button: String, onClick: () -> Unit) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LfPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = LfPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LfInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = LfMuted, fontSize = 10.sp)
            }
            TextButton(onClick = onClick) {
                Text(button, color = LfPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(selected: LfScreen, onSelect: (LfScreen) -> Unit) {
    val navItems = listOf(
        LfScreen.Home,
        LfScreen.Tasks,
        LfScreen.Editor,
        LfScreen.Run,
        LfScreen.History,
        LfScreen.Apps,
        LfScreen.Permissions,
        LfScreen.Import,
        LfScreen.Settings,
        LfScreen.More
    )
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { screen ->
                val active = screen == selected
                TextButton(onClick = { onSelect(screen) }, modifier = Modifier.width(72.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(screen.symbol, color = if (active) LfPrimary else LfMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(screen.label, color = if (active) LfPrimary else LfMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, background: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CardBlock(color: Color = LfCard, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DarkPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142638)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DarkChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0xFF24384A))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(text, color = Color(0xFFB8C6D5), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    CardBlock {
        Text(title, color = LfInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = LfMuted, fontSize = 11.sp)
    }
}

@Composable
fun PrimaryActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LfPrimary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DangerActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, color = LfDanger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ScheduleType.label(): String = when (this) {
    ScheduleType.MANUAL -> "手动"
    ScheduleType.DAILY -> "每日"
    ScheduleType.WEEKLY -> "每周"
    ScheduleType.LOOP -> "循环"
    ScheduleType.RUN_COUNT -> "次数"
}

private fun FailurePolicy.label(): String = when (this) {
    FailurePolicy.STOP -> "失败停止"
    FailurePolicy.RETRY -> "自动重试"
    FailurePolicy.SKIP -> "跳过继续"
}

private fun ActionType.label(): String = when (this) {
    ActionType.TAP_COORDINATE -> "点击坐标"
    ActionType.TAP_TEXT -> "点击文字"
    ActionType.SWIPE -> "滑动"
    ActionType.INPUT_TEXT -> "输入"
    ActionType.BACK -> "返回"
    ActionType.OPEN_APP -> "打开应用"
    ActionType.LONG_PRESS -> "长按"
    ActionType.RUN_SUBTASK -> "子任务"
    ActionType.WAIT -> "等待"
    ActionType.WAIT_FOR_TEXT -> "等文字"
    ActionType.WAIT_FOR_SCREEN -> "等页面"
}

private fun RunStatus.label(): String = when (this) {
    RunStatus.IDLE -> "待机"
    RunStatus.RUNNING -> "运行中"
    RunStatus.PAUSED -> "已暂停"
    RunStatus.CANCELLING -> "正在取消"
    RunStatus.COMPLETED -> "已完成"
    RunStatus.FAILED -> "失败"
    RunStatus.CANCELLED -> "已取消"
}
