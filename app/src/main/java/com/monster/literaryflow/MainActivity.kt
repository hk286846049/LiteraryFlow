package com.monster.literaryflow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.ClipData
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.requireAccessibility
import cn.coderpig.cp_fast_accessibility.shortToast
import com.monster.literaryflow.automation.action.ActionExecutionContext
import com.monster.literaryflow.automation.action.AndroidActionExecutor
import com.monster.literaryflow.automation.engine.ExecutionEngine
import com.monster.literaryflow.automation.action.OptionalShizukuActionExecutor
import com.monster.literaryflow.automation.state.RunState
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.StepType
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.model.AppProfileModel
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.data.repository.LiteraryFlow2Repository
import com.monster.literaryflow.data.repository.RunRecordRepository
import com.monster.literaryflow.service.CaptureService
import com.monster.literaryflow.service.FloatingWindowService
import com.monster.literaryflow.service.SharedData
import com.monster.literaryflow.ui2.LiteraryFlow2App
import com.monster.literaryflow.ui2.LiteraryFlow2Theme
import com.monster.literaryflow.ui2.InstalledAppOption
import com.monster.literaryflow.core.model.OcrResultModel
import com.monster.literaryflow.core.result.FlowResult
import com.monster.literaryflow.vision.ocr.MlKitOcrEngine
import com.monster.literaryflow.vision.ocr.OcrOptions
import com.monster.literaryflow.utils.AppUtils
import kotlinx.coroutines.launch

@SuppressLint("WrongConstant")
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1
        const val REQUEST_CODE_OVERLAY_PERMISSION = 2
    }

    private var findHorText = false
    private val repository by lazy { LiteraryFlow2Repository(applicationContext) }
    private val runRecordRepository by lazy { RunRecordRepository(applicationContext) }
    private val settingsPreferences by lazy { getSharedPreferences("literary_flow_settings", MODE_PRIVATE) }
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private var documentState by mutableStateOf(AutomationDocument())
    private var runState by mutableStateOf(RunState())
    private var loadingState by mutableStateOf(true)
    private var importMessageState by mutableStateOf<String?>(null)
    private var installedAppsState by mutableStateOf<List<InstalledAppOption>>(emptyList())
    private var runRecordsState by mutableStateOf<List<com.monster.literaryflow.data.db2.RunRecordEntity2>>(emptyList())
    private var stepRecordsState by mutableStateOf<List<com.monster.literaryflow.data.db2.StepRecordEntity2>>(emptyList())
    private var ocrDebugResultState by mutableStateOf<OcrResultModel?>(null)
    private var ocrDebugBitmapState by mutableStateOf<android.graphics.Bitmap?>(null)
    private var ocrDebugBusyState by mutableStateOf(false)
    private var permissionRefresh by mutableStateOf(0)
    private var settingsState by mutableStateOf(defaultSettingsState())
    private var numericSettingsState by mutableStateOf(defaultNumericSettingsState())
    private var activeRunRecordId: Long? = null
    private var activeStepRecordId: Long? = null
    private var activeStepIndex: Int = -1
    private val executionEngine by lazy {
        ExecutionEngine(
            actionExecutor = OptionalShizukuActionExecutor(AndroidActionExecutor(applicationContext)),
            taskResolver = { id, title ->
                documentState.tasks.firstOrNull { task ->
                    (id != null && task.id == id) || (title != null && task.title == title)
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsState = loadSettingsState()
        numericSettingsState = loadNumericSettingsState()
        if (settingsState["keepScreenOn"] == true) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        observeSharedData()
        setContent {
            LiteraryFlow2Theme {
                LiteraryFlow2App(
                    document = documentState,
                    installedApps = installedAppsState,
                    runState = runState,
                    runRecords = runRecordsState,
                    stepRecords = stepRecordsState,
                    ocrDebugResult = ocrDebugResultState,
                    ocrDebugBitmap = ocrDebugBitmapState,
                    ocrDebugBusy = ocrDebugBusyState,
                    accessibilityGranted = isAccessibilityEnable,
                    overlayGranted = Settings.canDrawOverlays(this),
                    screenCaptureGranted = MyApp.mediaProjection != null || MyApp.image.value != null,
                    batteryOptimizationGranted = (getSystemService(POWER_SERVICE) as android.os.PowerManager).isIgnoringBatteryOptimizations(packageName),
                    permissionVersion = permissionRefresh,
                    settings = settingsState,
                    numericSettings = numericSettingsState,
                    loading = loadingState,
                    importMessage = importMessageState,
                    onStartCapture = {
                        findHorText = false
                        // Keep the reader attached to the existing virtual display when capture
                        // permission is already active. Replacing it here would leave the display
                        // writing to the old surface and OCR would see no new frames.
                        if (MyApp.imageReader == null) {
                            MyApp.imageReader = AppUtils.initImageReader(this@MainActivity)
                        }
                        startScreenCapture()
                    },
                    onStopCapture = ::stopScreenCapture,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onOpenBatterySettings = ::openBatterySettings,
                    onAddAction = ::addAction,
                    onAddConfiguredStep = ::addConfiguredStep,
                    onUpdateStep = ::updateStep,
                    onPauseRun = executionEngine::pause,
                    onResumeRun = executionEngine::resume,
                    onCreateTask = ::createTask,
                    onCreateTemplate = ::createTemplate,
                    onDeleteTask = ::deleteTask,
                    onToggleTaskEnabled = ::toggleTaskEnabled,
                    onToggleTaskFavorite = ::toggleTaskFavorite,
                    onDuplicateTask = ::duplicateTask,
                    onBatchExport = ::exportSelectedTasks,
                    onToggleAppFavorite = ::toggleAppFavorite,
                    onToggleAppOrientation = ::toggleAppOrientation,
                    onSettingChanged = ::setSetting,
                    onNumericSettingChanged = ::setNumericSetting,
                    onRunOcrDebug = ::runOcrDebug,
                    onClearHistory = ::clearHistory,
                    onLoadRunDetail = ::loadRunDetail,
                    onUpdateTask = ::updateTask,
                    onDeleteStep = ::deleteStep,
                    onSetTargetApp = ::setTargetApp,
                    onAddWaitStep = ::addWaitStep,
                    onRenameTask = ::renameTask,
                    onRunTask = ::runTask,
                    onCancelRun = executionEngine::cancel,
                    onExportJson = ::exportJson,
                    onImportJson = ::importJson
                )
            }
        }
        lifecycleScope.launch {
            executionEngine.state.collect { state ->
                runState = state
                when {
                    state.status == com.monster.literaryflow.automation.state.RunStatus.RUNNING &&
                        activeRunRecordId == null &&
                        state.taskId != null -> {
                        activeRunRecordId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runRecordRepository.start(state.taskId)
                        }
                    }
                    state.status in setOf(
                        com.monster.literaryflow.automation.state.RunStatus.COMPLETED,
                        com.monster.literaryflow.automation.state.RunStatus.FAILED,
                        com.monster.literaryflow.automation.state.RunStatus.CANCELLED
                    ) -> {
                        val recordId = activeRunRecordId
                        if (recordId != null) {
                            state.taskId?.let { taskId ->
                                val task = documentState.tasks.firstOrNull { it.id == taskId }
                                if (task != null && state.status == com.monster.literaryflow.automation.state.RunStatus.COMPLETED) {
                                    persistDocument(documentState.copy(tasks = documentState.tasks.map {
                                        if (it.id == taskId) it.copy(todayRunCount = it.todayRunCount + 1, estimatedDurationMs = (System.currentTimeMillis() - (activeRunRecordId ?: System.currentTimeMillis())).coerceAtLeast(0L), updatedAt = System.currentTimeMillis()) else it
                                    }))
                                }
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                activeStepRecordId?.let { stepId ->
                                    runRecordRepository.finishStep(stepId, state.status.name, state.message)
                                }
                                runRecordRepository.finish(recordId, state.status.name, state.message)
                                runRecordsState = runRecordRepository.recent()
                                stepRecordsState = runRecordRepository.steps(recordId)
                            }
                            activeRunRecordId = null
                            activeStepRecordId = null
                            activeStepIndex = -1
                        }
                    }
                }
                val runId = activeRunRecordId
                if (runId != null && state.status == com.monster.literaryflow.automation.state.RunStatus.RUNNING) {
                    val task = state.taskId?.let { id -> documentState.tasks.firstOrNull { it.id == id } }
                    if (task != null && state.currentStepIndex != activeStepIndex) {
                        val previous = activeStepRecordId
                        if (previous != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runRecordRepository.finishStep(previous, "COMPLETED", null)
                            }
                        }
                        val step = task.steps.getOrNull(state.currentStepIndex)
                        activeStepRecordId = if (step != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runRecordRepository.startStep(runId, step.id)
                            }
                        } else null
                        activeStepIndex = state.currentStepIndex
                    }
                }
            }
        }
        reloadDocument()
        loadInstalledApps()
        loadRunRecords()
    }

    private fun defaultSettingsState(): Map<String, Boolean> = mapOf(
        "autoOpenApp" to true,
        "retryOnFail" to false,
        "floatingController" to true,
        "vibration" to true,
        "keepScreenOn" to false,
        "enhanceOcr" to true
    )

    private fun defaultNumericSettingsState(): Map<String, Int> = mapOf(
        "ocrConfidence" to 85,
        "floatingOpacity" to 85
    )

    private fun loadSettingsState(): Map<String, Boolean> = mapOf(
        "autoOpenApp" to settingsPreferences.getBoolean("autoOpenApp", true),
        "retryOnFail" to settingsPreferences.getBoolean("retryOnFail", false),
        "floatingController" to settingsPreferences.getBoolean("floatingController", true),
        "vibration" to settingsPreferences.getBoolean("vibration", true),
        "keepScreenOn" to settingsPreferences.getBoolean("keepScreenOn", false),
        "enhanceOcr" to settingsPreferences.getBoolean("enhanceOcr", true)
    )

    private fun loadNumericSettingsState(): Map<String, Int> = mapOf(
        "ocrConfidence" to settingsPreferences.getInt("ocrConfidence", 85),
        "floatingOpacity" to settingsPreferences.getInt("floatingOpacity", 85)
    )

    private fun observeSharedData() {
        SharedData.trigger.observe(this) { value ->
            Log.d("MainActivity", "trigger: $value")
            when (value) {
                "打开录屏|竖屏" -> {
                    findHorText = false
                    MyApp.imageReader = AppUtils.initImageReader(this)
                    startScreenCapture()
                }
                "打开录屏|横屏" -> {
                    findHorText = true
                    MyApp.imageReader = AppUtils.initImageReader(this, true)
                    startScreenCapture()
                }
            }
            SharedData.trigger.postValue("")
        }
    }

    private fun reloadDocument() {
        loadingState = true
        lifecycleScope.launch {
            documentState = repository.loadDocument()
            loadingState = false
            importMessageState = if (documentState.tasks.isEmpty()) {
                "当前没有任务。可在导入页粘贴旧版 AutoInfo JSON。"
            } else {
                "已加载 ${documentState.tasks.size} 个新版任务。"
            }
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(0)
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .map {
                    InstalledAppOption(
                        label = packageManager.getApplicationLabel(it).toString(),
                        packageName = it.packageName
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            installedAppsState = apps
        }
    }

    private fun loadRunRecords() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runRecordsState = runRecordRepository.recent()
        }
    }

    private fun setSetting(key: String, value: Boolean) {
        settingsState = settingsState + (key to value)
        settingsPreferences.edit().putBoolean(key, value).apply()
        if (key == "keepScreenOn") {
            if (value) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setNumericSetting(key: String, value: Int) {
        numericSettingsState = numericSettingsState + (key to value)
        settingsPreferences.edit().putInt(key, value).apply()
    }

    private fun runOcrDebug() {
        val bitmap = MyApp.image.value
        if (bitmap == null) {
            importMessageState = "请先启动屏幕捕获，再运行 OCR 调试。"
            return
        }
        ocrDebugBitmapState = bitmap
        ocrDebugBusyState = true
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val engine = MlKitOcrEngine()
            val result = engine.recognize(bitmap, OcrOptions())
            engine.release()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                ocrDebugBusyState = false
                if (result is FlowResult.Success) ocrDebugResultState = result.value
                else if (result is FlowResult.Failure) importMessageState = result.error.message
            }
        }
    }

    private fun clearHistory() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runRecordRepository.clear()
            runRecordsState = emptyList()
            stepRecordsState = emptyList()
        }
    }

    private fun loadRunDetail(runRecordId: Long) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            stepRecordsState = runRecordRepository.steps(runRecordId)
        }
    }

    private fun importJson(json: String) {
        if (json.isBlank()) {
            importMessageState = "请先粘贴新版或旧版 JSON。"
            return
        }
        loadingState = true
        lifecycleScope.launch {
            when (val result = repository.importJson(json)) {
                is FlowResult.Success -> {
                    documentState = result.value.document
                    val warnings = result.value.warnings.joinToString(separator = "\n")
                    importMessageState = buildString {
                        append("已从 ${result.value.source} 转换 ${result.value.convertedTaskCount} 个任务。")
                        if (warnings.isNotBlank()) {
                            append("\n")
                            append(warnings)
                        }
                    }
                }
                is FlowResult.Failure -> {
                    importMessageState = result.error.message
                }
            }
            loadingState = false
        }
    }

    private fun runTask(task: TaskModel) {
        if (task.steps.isEmpty()) {
            importMessageState = "任务「${task.title}」没有可执行步骤。"
            return
        }
        val metrics = resources.displayMetrics
        executionEngine.start(
            task,
            ActionExecutionContext(
                taskId = task.id,
                stepId = 0L,
                screenWidth = metrics.widthPixels,
                screenHeight = metrics.heightPixels,
                packageName = task.targetPackageName,
                autoOpenTargetApp = task.allowAutoOpenApp
            )
        )
        importMessageState = "正在运行任务：${task.title}"
    }

    private fun newTask(): TaskModel {
        val now = System.currentTimeMillis()
        return TaskModel(
            id = now,
            title = "新任务",
            enabled = false,
            targetPackageName = "",
            targetAppName = "",
            allowAutoOpenApp = settingsPreferences.getBoolean("autoOpenApp", true),
            scheduleType = ScheduleType.MANUAL,
            startTime = null,
            endTime = null,
            weeklyDays = null,
            runTimes = 1,
            intervalMs = 0L,
            createdAt = now,
            updatedAt = now,
            steps = emptyList()
        )
    }

    private fun createTask() {
        val task = newTask()
        persistDocument(documentState.copy(tasks = documentState.tasks + task))
        importMessageState = "已创建任务「${task.title}」，可在编排页编辑。"
    }

    private fun createTemplate(kind: String) {
        val created = newTask()
        persistDocument(documentState.copy(tasks = documentState.tasks + created))
        var current = created
        val actions = when (kind) {
            "签到" -> listOf(
                ActionSpec(ActionType.WAIT_FOR_TEXT, text = "签到", timeoutMs = 15000L),
                ActionSpec(ActionType.TAP_TEXT, text = "签到", recognitionSource = com.monster.literaryflow.core.model.RecognitionSource.AUTO),
                ActionSpec(ActionType.WAIT_FOR_TEXT, text = "领取", timeoutMs = 10000L),
                ActionSpec(ActionType.TAP_TEXT, text = "领取", recognitionSource = com.monster.literaryflow.core.model.RecognitionSource.AUTO)
            )
            "广告" -> listOf(
                ActionSpec(ActionType.WAIT_FOR_TEXT, text = "跳过#关闭", matchType = com.monster.literaryflow.core.model.MatchType.ANY_KEYWORD, timeoutMs = 35000L),
                ActionSpec(ActionType.TAP_TEXT, text = "关闭", recognitionSource = com.monster.literaryflow.core.model.RecognitionSource.OCR)
            )
            else -> listOf(ActionSpec(ActionType.WAIT_FOR_SCREEN, text = "首页", recognitionSource = com.monster.literaryflow.core.model.RecognitionSource.OCR, timeoutMs = 8000L))
        }
        actions.forEach { action ->
            addAction(current, action)
            current = documentState.tasks.firstOrNull { it.id == created.id } ?: current
        }
        importMessageState = "已从“$kind”模板创建步骤，可在向导中修改。"
    }

    private fun addWaitStep(task: TaskModel) {
        val stepId = (task.steps.maxOfOrNull { it.id } ?: task.id * 10_000L) + 1L
        val step = StepModel(
            id = stepId,
            taskId = task.id,
            orderIndex = task.steps.size,
            type = StepType.WAIT,
            title = "等待 1 秒",
            timeoutMs = 1000L,
            delayAfterMs = 0L,
            failurePolicy = FailurePolicy.STOP,
            actions = listOf(ActionSpec(type = ActionType.WAIT, timeoutMs = 1000L))
        )
        val updated = task.copy(steps = task.steps + step, updatedAt = System.currentTimeMillis())
        val tasks = documentState.tasks.map { if (it.id == task.id) updated else it }
        persistDocument(documentState.copy(tasks = tasks))
        importMessageState = "已为「${task.title}」添加等待步骤。"
    }

    private fun addAction(task: TaskModel, action: ActionSpec) {
        val stepId = (task.steps.maxOfOrNull { it.id } ?: task.id * 10_000L) + 1L
        val title = when (action.type) {
            ActionType.TAP_TEXT -> "点击文字"
            ActionType.WAIT_FOR_TEXT -> "等待文字"
            ActionType.WAIT_FOR_SCREEN -> "等待页面"
            ActionType.TAP_COORDINATE -> "点击坐标"
            ActionType.SWIPE -> "屏幕滑动"
            ActionType.INPUT_TEXT -> "输入文字"
            ActionType.BACK -> "返回"
            ActionType.OPEN_APP -> "打开应用"
            ActionType.RUN_SUBTASK -> "执行子任务"
            ActionType.LONG_PRESS -> "长按"
            ActionType.WAIT -> "固定等待"
        }
        val step = StepModel(
            id = stepId,
            taskId = task.id,
            orderIndex = task.steps.size,
            type = if (action.type == ActionType.WAIT_FOR_TEXT || action.type == ActionType.WAIT_FOR_SCREEN) StepType.CONDITION else StepType.ACTION,
            title = title,
            timeoutMs = action.timeoutMs,
            delayAfterMs = 0L,
            failurePolicy = FailurePolicy.STOP,
            actions = if (action.type == ActionType.WAIT_FOR_SCREEN && !action.excludedText.isNullOrBlank()) emptyList() else listOf(action),
            conditions = if (action.type == ActionType.WAIT_FOR_SCREEN && !action.excludedText.isNullOrBlank()) {
                listOf(com.monster.literaryflow.core.model.ConditionSpec(
                    type = com.monster.literaryflow.core.model.ConditionType.SCREEN_STATE,
                    text = action.text,
                    excludedText = action.excludedText,
                    matchType = action.matchType,
                    recognitionSource = action.recognitionSource,
                    timeoutMs = action.timeoutMs
                ))
            } else emptyList()
        )
        val updated = task.copy(steps = task.steps + step, updatedAt = System.currentTimeMillis())
        persistDocument(documentState.copy(tasks = documentState.tasks.map { if (it.id == task.id) updated else it }))
        importMessageState = "已添加步骤：$title"
    }

    private fun addConfiguredStep(task: TaskModel, draft: StepModel) {
        val stepId = (task.steps.maxOfOrNull { it.id } ?: task.id * 10_000L) + 1L
        val step = draft.copy(
            id = stepId,
            taskId = task.id,
            orderIndex = task.steps.size
        )
        val updated = task.copy(steps = task.steps + step, updatedAt = System.currentTimeMillis())
        persistDocument(documentState.copy(tasks = documentState.tasks.map { if (it.id == task.id) updated else it }))
        importMessageState = "已添加并保存步骤：${step.title}"
    }

    private fun updateStep(task: TaskModel, step: StepModel) {
        val updated = task.copy(
            steps = task.steps.map { if (it.id == step.id) step.copy(taskId = task.id) else it },
            updatedAt = System.currentTimeMillis()
        )
        persistDocument(documentState.copy(tasks = documentState.tasks.map { if (it.id == task.id) updated else it }))
        importMessageState = "已更新步骤：${step.title}"
    }

    private fun setTargetApp(task: TaskModel, appName: String, packageName: String) {
        val updated = task.copy(
            targetAppName = appName,
            targetPackageName = packageName,
            updatedAt = System.currentTimeMillis()
        )
        val hasProfile = documentState.appProfiles.any { it.packageName == packageName }
        val profiles = if (hasProfile) documentState.appProfiles else documentState.appProfiles + AppProfileModel(
            id = System.currentTimeMillis(),
            packageName = packageName,
            appName = appName,
            defaultRecognitionSource = RecognitionSource.AUTO
        )
        persistDocument(documentState.copy(tasks = documentState.tasks.map { if (it.id == task.id) updated else it }, appProfiles = profiles))
        importMessageState = "已绑定目标应用：$packageName"
    }

    private fun deleteTask(task: TaskModel) {
        if (runState.taskId == task.id) {
            executionEngine.cancel()
        }
        persistDocument(documentState.copy(tasks = documentState.tasks.filterNot { it.id == task.id }))
        importMessageState = "已删除任务：${task.title}"
    }

    private fun toggleTaskEnabled(task: TaskModel, enabled: Boolean) {
        persistDocument(documentState.copy(tasks = documentState.tasks.map {
            if (it.id == task.id) it.copy(enabled = enabled, updatedAt = System.currentTimeMillis()) else it
        }))
    }

    private fun toggleTaskFavorite(task: TaskModel) {
        persistDocument(documentState.copy(tasks = documentState.tasks.map {
            if (it.id == task.id) it.copy(favorite = !it.favorite, updatedAt = System.currentTimeMillis()) else it
        }))
    }

    private fun toggleAppFavorite(packageName: String) {
        val existing = documentState.appProfiles.firstOrNull { it.packageName == packageName }
        val app = installedAppsState.firstOrNull { it.packageName == packageName } ?: return
        val profiles = if (existing == null) {
            documentState.appProfiles + AppProfileModel(
                id = System.currentTimeMillis(),
                packageName = packageName,
                appName = app.label,
                defaultRecognitionSource = RecognitionSource.AUTO,
                favorite = true
            )
        } else {
            documentState.appProfiles.map { if (it.packageName == packageName) it.copy(favorite = !it.favorite) else it }
        }
        persistDocument(documentState.copy(appProfiles = profiles))
    }

    private fun toggleAppOrientation(packageName: String, landscape: Boolean) {
        val app = installedAppsState.firstOrNull { it.packageName == packageName } ?: return
        val existing = documentState.appProfiles.firstOrNull { it.packageName == packageName }
        val profiles = if (existing == null) {
            documentState.appProfiles + AppProfileModel(
                id = System.currentTimeMillis(),
                packageName = packageName,
                appName = app.label,
                defaultRecognitionSource = RecognitionSource.AUTO,
                landscape = landscape
            )
        } else {
            documentState.appProfiles.map { if (it.packageName == packageName) it.copy(landscape = landscape) else it }
        }
        persistDocument(documentState.copy(appProfiles = profiles))
    }

    private fun duplicateTask(task: TaskModel) {
        val now = System.currentTimeMillis()
        val copy = task.copy(id = now, title = "${task.title} 副本", createdAt = now, updatedAt = now, todayRunCount = 0, favorite = false,
            steps = task.steps.mapIndexed { index, step -> step.copy(id = now * 10_000L + index + 1, taskId = now, orderIndex = index) })
        persistDocument(documentState.copy(tasks = listOf(copy) + documentState.tasks))
    }

    private fun updateTask(task: TaskModel) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        persistDocument(documentState.copy(tasks = documentState.tasks.map {
            if (it.id == task.id) updated else it
        }))
    }

    private fun deleteStep(task: TaskModel, step: StepModel) {
        val remaining = task.steps
            .filterNot { it.id == step.id }
            .mapIndexed { index, item -> item.copy(orderIndex = index) }
        val updated = task.copy(steps = remaining, updatedAt = System.currentTimeMillis())
        persistDocument(documentState.copy(tasks = documentState.tasks.map { if (it.id == task.id) updated else it }))
        importMessageState = "已删除步骤：${step.title}"
    }

    private fun renameTask(task: TaskModel, title: String) {
        val updated = task.copy(title = title, updatedAt = System.currentTimeMillis())
        val tasks = documentState.tasks.map { if (it.id == task.id) updated else it }
        persistDocument(documentState.copy(tasks = tasks))
        importMessageState = "任务名称已保存。"
    }

    private fun persistDocument(document: AutomationDocument) {
        documentState = document
        lifecycleScope.launch {
            repository.replaceDocument(document)
        }
    }

    private fun exportJson() {
        val json = com.monster.literaryflow.data.importexport.AutomationImportExport
            .exportDocument(documentState)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LiteraryFlow 2.0 JSON", json))
        importMessageState = "已导出 ${documentState.tasks.size} 个任务到剪贴板。"
    }

    private fun exportSelectedTasks(tasks: List<TaskModel>) {
        val selected = documentState.copy(tasks = tasks)
        val json = com.monster.literaryflow.data.importexport.AutomationImportExport.exportDocument(selected)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LiteraryFlow 2.0 selected tasks", json))
        importMessageState = "已导出 ${tasks.size} 个选中任务到剪贴板。"
    }

    override fun onDestroy() {
        executionEngine.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        permissionRefresh++
    }

    private fun openAccessibilitySettings() {
        if (!isAccessibilityEnable) {
            requireAccessibility()
        } else {
            shortToast("无障碍服务已开启")
        }
    }

    private fun openOverlaySettings() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        } else {
            startFloatingWindowService()
            shortToast("悬浮窗服务已启动")
        }
    }

    private fun openBatterySettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }.onFailure {
            shortToast("无法打开电池优化设置")
        }
    }

    private fun startScreen(data: Intent?) {
        val serviceIntent = Intent(this, CaptureService::class.java).apply {
            putExtra("resultCode", Activity.RESULT_OK)
            putExtra("data", data)
            putExtra("findHorText", findHorText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK) {
                    startScreen(data)
                    Log.d("MainActivity", "Starting screen capture")
                }
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingWindowService()
                }
            }
        }
    }

    private fun startScreenCapture() {
        if (MyApp.mediaProjection == null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            )
        }
    }

    private fun startFloatingWindowService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, FloatingWindowService::class.java))
        } else {
            startService(Intent(this, FloatingWindowService::class.java))
        }
    }

    private fun stopScreenCapture() {
        MyApp.mediaProjection?.stop()
        MyApp.virtualDisplay?.release()
        MyApp.virtualDisplay = null
        MyApp.mediaProjection = null
    }
}
