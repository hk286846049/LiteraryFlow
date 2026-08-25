package com.monster.literaryflow.ui2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.automation.state.RunState
import com.monster.literaryflow.automation.state.RunStatus
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.NormalizedRect
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.SwipeSpec
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.core.model.OcrResultModel

enum class ExtendedPage(
    val code: String,
    val title: String,
    val subtitle: String,
    val dark: Boolean = false
) {
    FULLSCREEN_OCR("06", "全屏文字识别", "全屏 OCR 选择识别目标"),
    FULLSCREEN_PAGE("07", "页面条件", "全屏识别页面状态"),
    RUN_DETAIL("08", "执行记录", "查看当前运行状态与最近结果"),
    SYSTEM_SETTINGS("09", "系统设置", "运行策略、权限和资源设置"),
    ADD_STEP("10", "添加动作菜单", "选择一个动作或感知条件，作为下一步"),
    APP_PICKER("11", "目标应用", "手游、广告工具或其他前台应用"),
    CLICK_TEXT("12", "点击文字配置", "识别目标文字并模拟点击"),
    WAIT_TEXT("13", "等待文字配置", "持续文字识别直到目标出现"),
    WAIT_PAGE("14", "页面状态条件", "组合必须/排除词判断页面"),
    COORDINATE("15", "坐标拾取", "记录归一化 X / Y"),
    ROI("16", "ROI 区域", "限制截图与 OCR 的识别范围"),
    GESTURE("17", "手势输入", "上下左右或自定义轨迹"),
    ADVANCED("18", "高级选项", "超时、失败处理和重试策略"),
    OCR_DEBUG("19", "OCR 调试沙箱", "查看识别文本、bbox 和耗时"),
    SETTINGS_DETAIL("20", "悬浮与 OCR 设置", "运行状态、识别策略和资源释放"),
    OVERLAY_COLLAPSED("21", "悬浮球", "折叠状态下的运行入口", dark = true),
    OVERLAY_EXPANDED("22", "悬浮控制", "展开后暂停、继续和停止", dark = true),
    RUNNER_PAUSED("23", "暂停状态", "资源释放后的继续入口", dark = true),
    STOP_CONFIRM("24", "停止确认", "停止后保存运行记录", dark = true),
    TEMPLATES("25", "任务模板库", "选择任务预设模板"),
    IMPORT("26", "导入任务", "解析新版或旧版 JSON"),
    EXPORT("27", "导出任务", "复制完整 schema JSON"),
    HELP("28", "帮助教程", "从权限到 OCR 的使用说明"),
    ABOUT("29", "关于文流", "版本、架构和开源信息"),
    APP_DETAIL("30", "应用详情", "目标应用 Profile 与识别策略"),
    CONFIRM("31", "通用确认", "确认危险操作或放弃修改")
}

data class InstalledAppOption(
    val label: String,
    val packageName: String
)

@Composable
fun ExtendedPagesScreen(
    document: AutomationDocument,
    installedApps: List<InstalledAppOption>,
    runState: RunState,
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    onCreateTemplate: (String) -> Unit = { onCreateTask() },
    onSetTargetApp: (TaskModel, String, String) -> Unit,
    onAddAction: (TaskModel, ActionSpec) -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onCancelRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onStartCapture: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
    ,onUpdateTask: (TaskModel) -> Unit = {}
    ,ocrDebugResult: OcrResultModel? = null,
    ocrDebugBitmap: android.graphics.Bitmap? = null,
    ocrDebugBusy: Boolean = false,
    onRunOcrDebug: () -> Unit = {}
) {
    var selected by remember { mutableStateOf<ExtendedPage?>(null) }
    val task = document.tasks.firstOrNull()
    val page = selected
    if (page == null) {
        ExtendedPageIndex(onBack = onBack, onOpen = { selected = it })
    } else {
        ExtendedPageDetail(
            page = page,
            task = task,
            document = document,
            runState = runState,
            onBack = { selected = null },
            onCreateTask = onCreateTask,
            onCreateTemplate = onCreateTemplate,
            onSetTargetApp = onSetTargetApp,
            installedApps = installedApps,
            onAddAction = { action -> task?.let { onAddAction(it, action) } },
            onRunTask = { task?.let(onRunTask) },
            onCancelRun = onCancelRun,
            onPauseRun = onPauseRun,
            onResumeRun = onResumeRun,
            onStartCapture = onStartCapture,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onExportJson = onExportJson,
            onImportJson = onImportJson
            ,onUpdateTask = onUpdateTask
            ,ocrDebugResult = ocrDebugResult,
            ocrDebugBitmap = ocrDebugBitmap,
            ocrDebugBusy = ocrDebugBusy,
            onRunOcrDebug = onRunOcrDebug
        )
    }
}

@Composable
private fun ExtendedPageIndex(onBack: () -> Unit, onOpen: (ExtendedPage) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LfBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹", color = LfInk, fontSize = 30.sp) }
            Column {
                Text("详情与工具", color = LfInk, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("完整 Figwright 页面分组", color = LfMuted, fontSize = 11.sp)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
        ) {
            items(ExtendedPage.values().toList()) { page ->
                Card(
                    onClick = { onOpen(page) },
                    colors = CardDefaults.cardColors(containerColor = if (page.dark) LfDark else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (page.dark) LfPrimary else LfPrimarySoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(page.code, color = if (page.dark) Color.White else LfPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(page.title, color = if (page.dark) Color.White else LfInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(page.subtitle, color = if (page.dark) Color(0xFFB2C7D6) else LfMuted, fontSize = 11.sp)
                        }
                        Text("›", color = if (page.dark) Color(0xFFB2C7D6) else LfMuted, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtendedPageDetail(
    page: ExtendedPage,
    task: TaskModel?,
    document: AutomationDocument,
    runState: RunState,
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    onCreateTemplate: (String) -> Unit = { onCreateTask() },
    onSetTargetApp: (TaskModel, String, String) -> Unit,
    installedApps: List<InstalledAppOption>,
    onAddAction: (ActionSpec) -> Unit,
    onRunTask: () -> Unit,
    onCancelRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onStartCapture: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
    ,onUpdateTask: (TaskModel) -> Unit = {}
    ,ocrDebugResult: OcrResultModel? = null,
    ocrDebugBitmap: android.graphics.Bitmap? = null,
    ocrDebugBusy: Boolean = false,
    onRunOcrDebug: () -> Unit = {}
) {
    val background = if (page.dark) LfDark else LfBackground
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹", color = if (page.dark) Color.White else LfInk, fontSize = 30.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(page.title, color = if (page.dark) Color.White else LfInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(page.subtitle, color = if (page.dark) Color(0xFFB2C7D6) else LfMuted, fontSize = 11.sp)
            }
            Text(page.code, color = if (page.dark) LfPrimarySoft else LfPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        when (page) {
            ExtendedPage.FULLSCREEN_OCR -> OcrDebugDetail(onStartCapture, ocrDebugResult, ocrDebugBitmap, ocrDebugBusy, onRunOcrDebug)
            ExtendedPage.FULLSCREEN_PAGE -> PageConditionDetail(onAddAction)
            ExtendedPage.RUN_DETAIL -> RunDetailDetail(runState, onCancelRun)
            ExtendedPage.SYSTEM_SETTINGS -> SettingsDetail(onStartCapture, onOpenOverlaySettings)
            ExtendedPage.ADD_STEP -> AddStepDetail(onAddAction)
            ExtendedPage.APP_PICKER -> AppPickerDetail(task, installedApps, onSetTargetApp)
            ExtendedPage.CLICK_TEXT -> TextActionDetail("点击文字", ActionType.TAP_TEXT, onAddAction)
            ExtendedPage.WAIT_TEXT -> TextActionDetail("等待文字出现", ActionType.WAIT_FOR_TEXT, onAddAction)
            ExtendedPage.WAIT_PAGE -> PageConditionDetail(onAddAction)
            ExtendedPage.COORDINATE -> CoordinateDetail(onAddAction)
            ExtendedPage.ROI -> RoiDetail(onAddAction)
            ExtendedPage.GESTURE -> GestureDetail(onAddAction)
            ExtendedPage.ADVANCED -> AdvancedDetail(task, onUpdateTask)
            ExtendedPage.OCR_DEBUG -> OcrDebugDetail(onStartCapture, ocrDebugResult, ocrDebugBitmap, ocrDebugBusy, onRunOcrDebug)
            ExtendedPage.SETTINGS_DETAIL -> SettingsDetail(onStartCapture, onOpenOverlaySettings)
            ExtendedPage.OVERLAY_COLLAPSED -> OverlayCollapsedDetail(runState, onOpenOverlaySettings, onRunTask)
            ExtendedPage.OVERLAY_EXPANDED -> OverlayExpandedDetail(runState, onPauseRun, onResumeRun, onCancelRun)
            ExtendedPage.RUNNER_PAUSED -> PausedDetail(runState, onResumeRun)
            ExtendedPage.STOP_CONFIRM -> StopConfirmDetail(onCancelRun, onResumeRun)
            ExtendedPage.TEMPLATES -> TemplatesDetail(onCreateTemplate)
            ExtendedPage.IMPORT -> ImportDetail(onImportJson)
            ExtendedPage.EXPORT -> ExportDetail(document, onExportJson)
            ExtendedPage.HELP -> HelpDetail()
            ExtendedPage.ABOUT -> AboutDetail()
            ExtendedPage.APP_DETAIL -> AppDetail(task)
            ExtendedPage.CONFIRM -> ConfirmDetail(onBack)
        }
    }
}

@Composable
private fun RunDetailDetail(runState: RunState, onCancelRun: () -> Unit) {
    ExtendedScroll {
        DetailSection("当前运行") {
            Text(runState.taskTitle ?: "暂无运行任务", color = LfInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${runState.status.name} · ${runState.currentStepIndex}/${runState.totalSteps}",
                color = LfMuted,
                fontSize = 12.sp
            )
            runState.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = LfMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            DangerActionButton("停止当前任务", modifier = Modifier.fillMaxWidth(), onClick = onCancelRun)
        }
        DetailHint("每次启动、完成、失败和取消都会写入新版 RunRecord。")
    }
}

@Composable
private fun AddStepDetail(onAddAction: (ActionSpec) -> Unit) {
    val actions = listOf(
        "点击文字" to ActionSpec(type = ActionType.TAP_TEXT, text = "目标文字", matchType = MatchType.EXACT, recognitionSource = RecognitionSource.AUTO),
        "等待文字出现" to ActionSpec(type = ActionType.WAIT_FOR_TEXT, text = "目标文字", matchType = MatchType.EXACT, recognitionSource = RecognitionSource.AUTO),
        "等待指定页面" to ActionSpec(type = ActionType.WAIT_FOR_SCREEN, text = "页面关键词", matchType = MatchType.ANY_KEYWORD, recognitionSource = RecognitionSource.OCR),
        "点击指定坐标" to ActionSpec(type = ActionType.TAP_COORDINATE, x = 0.5f, y = 0.5f),
        "屏幕滑动" to ActionSpec(type = ActionType.SWIPE, swipe = SwipeSpec(0.5f, 0.75f, 0.5f, 0.25f)),
        "输入文字" to ActionSpec(type = ActionType.INPUT_TEXT, inputText = "输入内容"),
        "固定等待时间" to ActionSpec(type = ActionType.WAIT, timeoutMs = 1000L),
        "系统返回键" to ActionSpec(type = ActionType.BACK),
        "打开目标应用" to ActionSpec(type = ActionType.OPEN_APP),
        "执行子任务" to ActionSpec(type = ActionType.RUN_SUBTASK)
    )
    ExtendedScroll {
        DetailSection("视觉识别与文字动作") {
            actions.take(3).forEach { (label, action) ->
                DetailActionRow(label, if (label.contains("点击")) "识别目标文字并模拟点击" else "等待页面变化后继续") {
                    onAddAction(action)
                }
            }
        }
        DetailSection("手势与输入") {
            actions.drop(3).take(3).forEach { (label, action) ->
                DetailActionRow(label, "写入新版 ActionSpec") { onAddAction(action) }
            }
        }
        DetailSection("流程与系统控制") {
            actions.drop(6).forEach { (label, action) ->
                DetailActionRow(label, "保存到当前任务") { onAddAction(action) }
            }
        }
    }
}

@Composable
private fun TextActionDetail(label: String, type: ActionType, onAddAction: (ActionSpec) -> Unit) {
    var text by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(RecognitionSource.AUTO) }
    ExtendedScroll {
        DetailSection("步骤配置") {
            OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("目标文字") }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            DetailChoiceRow("识别策略", listOf("自动", "无障碍", "OCR"), listOf(RecognitionSource.AUTO, RecognitionSource.ACCESSIBILITY, RecognitionSource.OCR), source) { source = it }
            Spacer(Modifier.height(10.dp))
            PrimaryActionButton("保存步骤", modifier = Modifier.fillMaxWidth()) {
                onAddAction(ActionSpec(type = type, text = text.ifBlank { "目标文字" }, matchType = MatchType.EXACT, recognitionSource = source, timeoutMs = 5000L))
            }
        }
        DetailHint(if (type.name == "TAP_TEXT") "推荐优先使用文字识别动作，适配不同分辨率。" else "页面变化后重新识别，结果缓存到 ScreenState。")
    }
}

@Composable
private fun PageConditionDetail(onAddAction: (ActionSpec) -> Unit) {
    var required by remember { mutableStateOf("") }
    var excluded by remember { mutableStateOf("") }
    ExtendedScroll {
        DetailSection("页面进入策略") {
            OutlinedTextField(required, { required = it }, modifier = Modifier.fillMaxWidth(), label = { Text("必须出现的词") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(excluded, { excluded = it }, modifier = Modifier.fillMaxWidth(), label = { Text("排除词，可选") })
            Spacer(Modifier.height(8.dp))
            DetailHint("多个关键词用 # 分隔；页面变化后才重新 OCR。")
            PrimaryActionButton("保存页面条件", modifier = Modifier.fillMaxWidth()) {
                onAddAction(ActionSpec(type = ActionType.WAIT_FOR_SCREEN, text = required, excludedText = excluded, matchType = MatchType.ANY_KEYWORD, recognitionSource = RecognitionSource.OCR, timeoutMs = 8000L))
            }
        }
    }
}

@Composable
private fun CoordinateDetail(onAddAction: (ActionSpec) -> Unit) {
    var x by remember { mutableFloatStateOf(0.5f) }
    var y by remember { mutableFloatStateOf(0.5f) }
    ExtendedScroll {
        DetailSection("归一化坐标") {
            Text("X ${(x * 100).toInt()}%", color = LfInk, fontSize = 12.sp)
            Slider(x, { x = it })
            Text("Y ${(y * 100).toInt()}%", color = LfInk, fontSize = 12.sp)
            Slider(y, { y = it })
            PrimaryActionButton("保存坐标动作", modifier = Modifier.fillMaxWidth()) { onAddAction(ActionSpec(type = ActionType.TAP_COORDINATE, x = x, y = y)) }
        }
        DetailHint("执行时会结合屏幕尺寸、方向和 Insets 重新计算实际坐标。")
    }
}

@Composable
private fun RoiDetail(onAddAction: (ActionSpec) -> Unit) {
    var left by remember { mutableFloatStateOf(0f) }
    var top by remember { mutableFloatStateOf(0f) }
    var right by remember { mutableFloatStateOf(1f) }
    var bottom by remember { mutableFloatStateOf(1f) }
    ExtendedScroll {
        DetailSection("ROI 区域") {
            RoiSlider("左", left) { left = it.coerceAtMost(right) }
            RoiSlider("上", top) { top = it.coerceAtMost(bottom) }
            RoiSlider("右", right) { right = it.coerceAtLeast(left) }
            RoiSlider("下", bottom) { bottom = it.coerceAtLeast(top) }
            PrimaryActionButton("保存 ROI", modifier = Modifier.fillMaxWidth()) {
                onAddAction(ActionSpec(type = ActionType.WAIT_FOR_SCREEN, roi = NormalizedRect(left, top, right, bottom).normalized(), recognitionSource = RecognitionSource.OCR, timeoutMs = 5000L))
            }
        }
    }
}

@Composable
private fun GestureDetail(onAddAction: (ActionSpec) -> Unit) {
    ExtendedScroll {
        DetailSection("手势与输入") {
            val gestures: List<Pair<String, SwipeSpec>> = listOf(
                "上滑" to SwipeSpec(0.5f, 0.78f, 0.5f, 0.22f),
                "下滑" to SwipeSpec(0.5f, 0.22f, 0.5f, 0.78f),
                "左滑" to SwipeSpec(0.78f, 0.5f, 0.22f, 0.5f),
                "右滑" to SwipeSpec(0.22f, 0.5f, 0.78f, 0.5f)
            )
            gestures.forEach { gesture ->
                val label = gesture.first
                val swipe = gesture.second
                DetailActionRow(label, "400ms · 归一化轨迹") { onAddAction(ActionSpec(type = ActionType.SWIPE, swipe = swipe)) }
            }
        }
    }
}

@Composable
private fun AppPickerDetail(
    task: TaskModel?,
    installedApps: List<InstalledAppOption>,
    onSetTargetApp: (TaskModel, String, String) -> Unit
) {
    var query by remember { mutableStateOf(task?.targetPackageName.orEmpty()) }
    val filteredApps = installedApps.filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }.take(20)
    ExtendedScroll {
        DetailSection("目标应用") {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("应用名称或包名") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            filteredApps.forEach { app ->
                DetailActionRow(app.label, app.packageName) {
                    query = app.packageName
                    task?.let { onSetTargetApp(it, app.label, app.packageName) }
                }
            }
            DetailHint("当前任务目标：${task?.targetAppName ?: "未选择"}")
            PrimaryActionButton("使用当前包名", modifier = Modifier.fillMaxWidth()) {
                task?.let { onSetTargetApp(it, query.trim(), query.trim()) }
            }
        }
    }
}

@Composable
private fun AdvancedDetail(task: TaskModel?, onUpdateTask: (TaskModel) -> Unit) {
    var policy by remember(task?.id) { mutableStateOf(FailurePolicy.STOP) }
    var timeout by remember(task?.id) { mutableStateOf(task?.steps?.firstOrNull()?.timeoutMs?.toFloat() ?: 3000f) }
    ExtendedScroll {
        DetailSection("高级选项") {
            DetailChoiceRow("失败处理", listOf("失败即停止", "自动重试 3 次", "跳过继续"), listOf(FailurePolicy.STOP, FailurePolicy.RETRY, FailurePolicy.SKIP), policy) { policy = it }
            Spacer(Modifier.height(10.dp))
            Text("默认步骤超时 ${timeout.toLong()} ms", color = LfMuted, fontSize = 11.sp)
            androidx.compose.material3.Slider(value = timeout, onValueChange = { timeout = it }, valueRange = 500f..60000f)
            PrimaryActionButton("保存到当前任务", Modifier.fillMaxWidth(), onClick = {
                task?.let { current ->
                    onUpdateTask(current.copy(steps = current.steps.map { step -> step.copy(failurePolicy = policy, timeoutMs = timeout.toLong()) }, updatedAt = System.currentTimeMillis()))
                }
            })
            Spacer(Modifier.height(8.dp))
            DetailHint("任务 ${task?.title ?: "未选择"} 的每个 Step 都会独立记录超时、延迟和失败策略。")
        }
    }
}

@Composable
private fun OcrDebugDetail(
    onStartCapture: () -> Unit,
    result: OcrResultModel?,
    bitmap: android.graphics.Bitmap?,
    busy: Boolean,
    onRunOcr: () -> Unit
) {
    ExtendedScroll {
        DetailSection("OCR Debug Sandbox") {
            Text(if (bitmap == null) "当前帧：等待屏幕捕获" else "当前帧：${bitmap.width} × ${bitmap.height}", color = LfInk, fontSize = 13.sp)
            Text("文字块：${result?.blocks?.size ?: 0} · bbox：${result?.blocks?.size ?: 0} · 平均置信度：${result?.blocks?.map { it.confidence }?.average()?.let { "%.2f".format(it) } ?: "-"}", color = LfMuted, fontSize = 11.sp)
            result?.let { ocr ->
                Spacer(Modifier.height(8.dp))
                ocr.blocks.take(12).forEach { block ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(block.text, color = LfInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("(${block.boundingBox.left},${block.boundingBox.top})", color = LfMuted, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PrimaryActionButton("开始屏幕捕获", modifier = Modifier.fillMaxWidth(), onClick = onStartCapture)
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(if (busy) "OCR 识别中…" else "运行真实 OCR", modifier = Modifier.fillMaxWidth(), onClick = onRunOcr)
        }
        DetailHint("OCR 只在页面变化或动作明确要求时运行，结果写入缓存。")
    }
}

@Composable
private fun SettingsDetail(onStartCapture: () -> Unit, onOpenOverlaySettings: () -> Unit) {
    ExtendedScroll {
        DetailSection("悬浮与 OCR 设置") {
            DetailActionRow("悬浮窗权限", "显示运行状态与停止入口", onOpenOverlaySettings)
            DetailActionRow("屏幕捕获", "截图与文字识别", onStartCapture)
            DetailChoiceRow("默认感知模式", listOf("IDLE", "NORMAL", "FAST"), listOf("IDLE", "NORMAL", "FAST"), "NORMAL") {}
        }
    }
}

@Composable
private fun OverlayCollapsedDetail(runState: RunState, onOpenOverlaySettings: () -> Unit, onRunTask: () -> Unit) {
    DarkExtendedScroll {
        DarkDetail("悬浮球", runState.status.name, "展开后可暂停、继续或停止当前任务")
        PrimaryActionButton("打开悬浮窗设置", modifier = Modifier.fillMaxWidth(), onClick = onOpenOverlaySettings)
        Spacer(Modifier.height(10.dp))
        PrimaryActionButton("运行当前任务", modifier = Modifier.fillMaxWidth(), onClick = onRunTask)
    }
}

@Composable
private fun OverlayExpandedDetail(runState: RunState, onPauseRun: () -> Unit, onResumeRun: () -> Unit, onCancelRun: () -> Unit) {
    DarkExtendedScroll {
        DarkDetail("悬浮控制", runState.taskTitle ?: "待机", "${runState.currentStepIndex}/${runState.totalSteps} · ${runState.message ?: "无状态"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryActionButton("暂停", modifier = Modifier.weight(1f), onClick = onPauseRun)
            PrimaryActionButton("继续", modifier = Modifier.weight(1f), onClick = onResumeRun)
            DangerActionButton("停止", modifier = Modifier.weight(1f), onClick = onCancelRun)
        }
    }
}

@Composable
private fun PausedDetail(runState: RunState, onResumeRun: () -> Unit) {
    DarkExtendedScroll {
        DarkDetail("任务已暂停", runState.taskTitle ?: "待机", "屏幕与文字识别资源已释放，点击继续执行")
        PrimaryActionButton("▶ 继续执行", modifier = Modifier.fillMaxWidth(), onClick = onResumeRun)
    }
}

@Composable
private fun StopConfirmDetail(onCancelRun: () -> Unit, onResumeRun: () -> Unit) {
    DarkExtendedScroll {
        DarkDetail("停止当前任务？", "确认后会标记为已取消", "已完成步骤仍会保存到运行记录")
        DangerActionButton("停止并保存记录", modifier = Modifier.fillMaxWidth(), onClick = onCancelRun)
        Spacer(Modifier.height(10.dp))
        PrimaryActionButton("继续运行", modifier = Modifier.fillMaxWidth(), onClick = onResumeRun)
    }
}

@Composable
private fun TemplatesDetail(onCreateTemplate: (String) -> Unit) {
    ExtendedScroll {
        DetailSection("预设模板") {
            DetailActionRow("每日签到与福利", "等待“签到” → 点击文字 → 记录运行", { onCreateTemplate("签到") })
            DetailActionRow("广告关闭", "等待关闭/跳过 → OCR 点击", { onCreateTemplate("广告") })
            DetailActionRow("页面巡检", "页面关键词条件 → OCR Debug", { onCreateTemplate("巡检") })
        }
    }
}

@Composable
private fun ImportDetail(onImportJson: (String) -> Unit) {
    var json by remember { mutableStateOf("") }
    ExtendedScroll {
        DetailSection("导入任务") {
            OutlinedTextField(json, { json = it }, modifier = Modifier.fillMaxWidth().height(220.dp), label = { Text("新版或旧版 JSON") })
            Spacer(Modifier.height(10.dp))
            PrimaryActionButton("解析并导入", modifier = Modifier.fillMaxWidth()) { onImportJson(json) }
        }
    }
}

@Composable
private fun ExportDetail(document: AutomationDocument, onExportJson: () -> Unit) {
    ExtendedScroll {
        DetailSection("导出任务") {
            Text("当前 ${document.tasks.size} 个任务 · schemaVersion ${document.schemaVersion}", color = LfInk, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            PrimaryActionButton("复制 JSON 到剪贴板", modifier = Modifier.fillMaxWidth(), onClick = onExportJson)
        }
    }
}

@Composable
private fun HelpDetail() {
    ExtendedScroll {
        DetailSection("帮助教程") {
            listOf(
                "1. 先开启无障碍、悬浮窗和屏幕捕获权限。",
                "2. 创建任务并选择目标应用 Profile。",
                "3. 优先使用点击文字、等待文字和等待页面。",
                "4. 坐标和 ROI 使用 0~1 归一化值，换分辨率仍可重新计算。",
                "5. 运行中的任务可以暂停、恢复或取消。"
            ).forEach { text: String -> Text(text, color = LfMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 5.dp)) }
        }
    }
}

@Composable
private fun AboutDetail() {
    ExtendedScroll {
        DetailSection("关于文流 2.0") {
            Text("LiteraryFlow 2.0", color = LfInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Android 视觉感知自动化工具", color = LfMuted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text("Perception 与 Action 分离；OCR 结果缓存；任务配置和运行状态分离。", color = LfMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AppDetail(task: TaskModel?) {
    ExtendedScroll {
        DetailSection("应用详情") {
            Text(task?.targetAppName ?: "未选择应用", color = LfInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(task?.targetPackageName ?: "未绑定包名", color = LfMuted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            DetailHint("默认识别源：AUTO · 支持 Accessibility / OCR / Template；横竖屏由 ScreenMetrics 统一处理。")
        }
    }
}

@Composable
private fun ConfirmDetail(onBack: () -> Unit) {
    var continueEditing by remember { mutableStateOf(false) }
    ExtendedScroll {
        DetailSection("通用确认") {
            Text("确认放弃当前修改？", color = LfInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("未保存的步骤配置不会写入新版数据库。", color = LfMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            DangerActionButton("放弃修改", modifier = Modifier.fillMaxWidth(), onClick = onBack)
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton("继续编辑", modifier = Modifier.fillMaxWidth(), onClick = { continueEditing = true })
            if (continueEditing) {
                Text("已保留当前编辑内容，可从上方返回继续配置。", color = LfPrimary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ExtendedScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
private fun DarkExtendedScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LfDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        content = {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(title, color = LfMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    )
}

@Composable
fun DetailActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(LfPrimarySoft, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("＋", color = LfPrimary, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LfInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = LfMuted, fontSize = 10.sp)
            }
            Text("›", color = LfMuted, fontSize = 20.sp)
        }
    }
}

@Composable
fun <T> DetailChoiceRow(
    label: String,
    labels: List<String>,
    values: List<T>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Text(label, color = LfInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, item ->
            val active = values[index] == selected
            TextButton(
                onClick = { onSelected(values[index]) },
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) LfPrimarySoft else Color(0xFFF4F7F8), RoundedCornerShape(12.dp))
            ) { Text(item, color = if (active) LfPrimary else LfMuted, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun RoiSlider(label: String, value: Float, onValue: (Float) -> Unit) {
    Text("$label ${(value * 100).toInt()}%", color = LfMuted, fontSize = 11.sp)
    Slider(value, onValue)
}

@Composable
private fun DetailHint(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LfPrimarySoft),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) { Text(text, color = LfMuted, fontSize = 11.sp, modifier = Modifier.padding(12.dp)) }
}

@Composable
private fun DarkDetail(title: String, subtitle: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142638)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = LfPrimarySoft, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Divider(color = Color(0xFF334559))
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFFB2C7D6), fontSize = 11.sp)
        }
    }
}
