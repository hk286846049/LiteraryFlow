package com.monster.literaryflow.ui2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.OcrResultModel
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.TaskModel

@Composable
fun CreateTaskWizardScreen(
    initialTask: TaskModel?,
    allTasks: List<TaskModel>,
    installedApps: List<InstalledAppOption>,
    ocrResult: OcrResultModel?,
    initialStep: Int = 1,
    onStartCapture: () -> Unit,
    onRunOcrDebug: () -> Unit,
    onBack: () -> Unit,
    onSaveTask: (TaskModel) -> Unit
) {
    var currentWizardStep by remember { mutableIntStateOf(initialStep.coerceIn(1, 4)) }

    // Apps options setup
    val defaultAppOptions = remember {
        listOf(
            WizardAppOption("com.hypergryph.arknights", "明日方舟", "🏰", true, true, "10分钟前"),
            WizardAppOption("com.mihoyo.starrail", "崩坏：星穹铁道", "🚀", true, true, "昨天"),
            WizardAppOption("com.pangle.adreward", "广告领宝箱 (AdRewards)", "🎁", false, false, "3天前"),
            WizardAppOption("com.kuaishou.nebula", "短视频自动宝箱", "🎬", false, false, "1小时前"),
            WizardAppOption("com.tencent.tmgp.sgame", "王者荣耀", "⚔️", true, false, "5天前")
        )
    }

    val availableApps = remember(installedApps) {
        if (installedApps.isEmpty()) defaultAppOptions
        else installedApps.map { app ->
            val def = defaultAppOptions.firstOrNull { it.packageName == app.packageName }
            WizardAppOption(
                packageName = app.packageName,
                appName = app.label,
                icon = def?.icon ?: getAppCategoryIcon("game", app.label),
                landscape = def?.landscape ?: true,
                favorite = def?.favorite ?: false,
                lastUsedTime = def?.lastUsedTime ?: "近期使用"
            )
        }
    }

    // Step 1: Basic info
    var taskName by remember { mutableStateOf(initialTask?.title ?: "每日签到与奖励领取") }
    var category by remember { mutableStateOf(initialTask?.category?.ifBlank { "game" } ?: "game") }
    var selectedApp by remember {
        mutableStateOf(
            availableApps.firstOrNull { it.packageName == initialTask?.targetPackageName }
                ?: availableApps.firstOrNull()
                ?: defaultAppOptions.first()
        )
    }
    var autoOpenApp by remember { mutableStateOf(initialTask?.allowAutoOpenApp ?: true) }

    // Step 2: Schedule
    var scheduleType by remember { mutableStateOf(initialTask?.scheduleType ?: ScheduleType.DAILY) }
    var dailyStart by remember { mutableStateOf(initialTask?.startTime ?: "09:00") }
    var dailyEnd by remember { mutableStateOf(initialTask?.endTime ?: "23:00") }
    var maxRunsPerDay by remember { mutableStateOf(initialTask?.runTimes ?: 1) }
    var weeklyDays by remember {
        mutableStateOf(
            initialTask?.weeklyDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }
    var loopIntervalMins by remember {
        mutableStateOf(((initialTask?.intervalMs ?: 1800000L) / 60000L).toInt().coerceAtLeast(5))
    }

    // Step 3: Steps state with WizardStepUi
    var steps by remember {
        mutableStateOf<List<WizardStepUi>>(
            if (!initialTask?.steps.isNullOrEmpty()) {
                initialTask!!.steps.map { it.toWizardStepUi() }
            } else {
                listOf(
                    WizardStepUi(
                        uid = 1L,
                        kind = WizardStepKind.OPEN_APP,
                        name = "打开目标游戏",
                        appName = selectedApp.appName,
                        appPackage = selectedApp.packageName,
                        description = "自动拉起 ${selectedApp.appName}"
                    ),
                    WizardStepUi(
                        uid = 2L,
                        kind = WizardStepKind.WAIT_TEXT,
                        name = "等待出现“签到”",
                        targetText = "签到",
                        maxWaitSeconds = 15,
                        matchMode = MatchType.FUZZY
                    ),
                    WizardStepUi(
                        uid = 3L,
                        kind = WizardStepKind.CLICK_TEXT,
                        name = "点击“签到”",
                        targetText = "签到",
                        matchMode = MatchType.FUZZY
                    ),
                    WizardStepUi(
                        uid = 4L,
                        kind = WizardStepKind.WAIT_TEXT,
                        name = "等待“领取”奖励",
                        targetText = "领取",
                        maxWaitSeconds = 10,
                        matchMode = MatchType.EXACT
                    ),
                    WizardStepUi(
                        uid = 5L,
                        kind = WizardStepKind.CLICK_TEXT,
                        name = "点击“领取”",
                        targetText = "领取",
                        matchMode = MatchType.EXACT
                    ),
                    WizardStepUi(
                        uid = 6L,
                        kind = WizardStepKind.BACK,
                        name = "返回大厅",
                        description = "退出弹窗"
                    )
                )
            }
        )
    }

    var selectedStepUid by remember { mutableStateOf(steps.firstOrNull()?.uid ?: 1L) }

    // Modals visibility
    var isAppSelectOpen by remember { mutableStateOf(false) }
    var isAddStepMenuOpen by remember { mutableStateOf(false) }
    var isCoordPickerOpen by remember { mutableStateOf(false) }
    var isAreaPickerOpen by remember { mutableStateOf(false) }
    var isOcrPickerOpen by remember { mutableStateOf(false) }
    var isPageConditionOpen by remember { mutableStateOf(false) }

    val activeStep = steps.firstOrNull { it.uid == selectedStepUid } ?: steps.firstOrNull() ?: defaultWizardStep(
        WizardStepKind.CLICK_TEXT,
        1L,
        selectedApp.appName,
        selectedApp.packageName
    )

    // Helper step actions
    val handleUpdateStep: (WizardStepUi) -> Unit = { updated ->
        steps = steps.map { if (it.uid == updated.uid) updated else it }
    }

    val handleAddStep: (WizardStepKind) -> Unit = { kind ->
        val newUid = System.currentTimeMillis()
        val newStep = defaultWizardStep(kind, newUid, selectedApp.appName, selectedApp.packageName)
        steps = steps + newStep
        selectedStepUid = newUid
    }

    val handleDeleteStep: (Long) -> Unit = { uid ->
        if (steps.size > 1) {
            val remaining = steps.filter { it.uid != uid }
            steps = remaining
            if (selectedStepUid == uid) {
                selectedStepUid = remaining.firstOrNull()?.uid ?: 0L
            }
        }
    }

    val handleDuplicateStep: (Long) -> Unit = { uid ->
        val target = steps.firstOrNull { it.uid == uid }
        if (target != null) {
            val newUid = System.currentTimeMillis()
            val newStep = target.copy(uid = newUid, name = "${target.name} (副本)")
            val index = steps.indexOfFirst { it.uid == uid }
            val newSteps = steps.toMutableList()
            newSteps.add(index + 1, newStep)
            steps = newSteps
            selectedStepUid = newUid
        }
    }

    val handleFinalSave = {
        val targetTaskId = initialTask?.id ?: System.currentTimeMillis()
        val convertedSteps = steps.mapIndexed { idx, s ->
            s.toStepModel(
                taskId = targetTaskId,
                orderIndex = idx,
                stepId = if (s.uid > 0L) s.uid else System.currentTimeMillis() + idx
            )
        }
        val estimatedMs = (steps.size * 15 + 20) * 1000L
        val saved = TaskModel(
            id = targetTaskId,
            title = taskName.trim().ifBlank { "未命名自动化任务" },
            enabled = initialTask?.enabled ?: true,
            targetPackageName = selectedApp.packageName,
            targetAppName = selectedApp.appName,
            allowAutoOpenApp = autoOpenApp,
            scheduleType = scheduleType,
            startTime = if (scheduleType == ScheduleType.DAILY || scheduleType == ScheduleType.WEEKLY) dailyStart else null,
            endTime = if (scheduleType == ScheduleType.DAILY || scheduleType == ScheduleType.WEEKLY) dailyEnd else null,
            weeklyDays = if (scheduleType == ScheduleType.WEEKLY) weeklyDays.sorted().joinToString(",") else null,
            runTimes = if (scheduleType == ScheduleType.DAILY) maxRunsPerDay else 1,
            intervalMs = if (scheduleType == ScheduleType.LOOP) loopIntervalMins * 60000L else 1800000L,
            createdAt = initialTask?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = convertedSteps,
            category = category,
            favorite = initialTask?.favorite ?: false,
            estimatedDurationMs = estimatedMs,
            todayRunCount = initialTask?.todayRunCount ?: 0
        )
        onSaveTask(saved)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Wizard Header Bar
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LfSlate100)
                                    .clickable {
                                        if (currentWizardStep > 1) currentWizardStep--
                                        else onBack()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LfSlate700)
                            }
                            Column {
                                Text(
                                    if (initialTask != null) "编辑自动化任务" else "创建自动化任务",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = LfSlate900
                                )
                                Text(
                                    "步骤 $currentWizardStep / 4 · ${
                                        when (currentWizardStep) {
                                            1 -> "基本信息"
                                            2 -> "运行方式"
                                            3 -> "步骤编排"
                                            else -> "检查并保存"
                                        }
                                    }",
                                    fontSize = 11.sp,
                                    color = LfSlate500
                                )
                            }
                        }

                        BentoBadge(
                            "进度: ${currentWizardStep * 25}%",
                            LfEmerald50,
                            LfEmerald700,
                            LfEmerald200
                        )
                    }

                    // 4-Pill Step Indicator Tracker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1 to "1. 基本信息",
                            2 to "2. 运行方式",
                            3 to "3. 步骤编排",
                            4 to "4. 检查保存"
                        ).forEach { (sNum, sLabel) ->
                            val isCurrent = currentWizardStep == sNum
                            val isCompleted = currentWizardStep > sNum
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) LfEmerald600 else if (isCompleted) LfEmerald100 else LfSlate100,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { currentWizardStep = sNum }
                            ) {
                                Text(
                                    text = if (isCompleted) "✓ $sLabel" else sLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) Color.White else if (isCompleted) LfEmerald900 else LfSlate500,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 1: 基本信息与目标应用
        // ==========================================
        if (currentWizardStep == 1) {
            item {
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Task Name
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("任务名称", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                                Text("*", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfRose500)
                            }
                            Spacer(Modifier.height(6.dp))
                            LfInputField(
                                value = taskName,
                                onValueChange = { taskName = it },
                                placeholder = "例如：每日签到与物资领取",
                                bold = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Target App Card
                        Column {
                            Text("目标应用", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                            Spacer(Modifier.height(6.dp))
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White)
                                                .border(1.dp, LfSlate200, RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(selectedApp.icon, fontSize = 22.sp)
                                        }
                                        Column {
                                            Text(selectedApp.appName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                            Text(selectedApp.packageName, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = LfSlate400)
                                        }
                                    }

                                    BentoOutlinedButton(
                                        text = "更换应用",
                                        onClick = { isAppSelectOpen = true },
                                        modifier = Modifier.height(34.dp)
                                    )
                                }
                            }
                        }

                        // Category Pills
                        Column {
                            Text("任务用途分类", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("game", "🎮 手游日常", "签到、收获与派遣"),
                                    Triple("ad", "🎁 广告操作", "跳过广告与宝箱"),
                                    Triple("general", "⚡ 通用自动化", "页面点击与流程")
                                ).forEach { (cKey, cLabel, cDesc) ->
                                    val isSelected = category == cKey
                                    BentoCard(
                                        backgroundColor = if (isSelected) LfEmerald50.copy(alpha = 0.8f) else LfSlate50,
                                        borderColor = if (isSelected) LfEmerald500 else LfSlate200,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(10.dp),
                                        onClick = { category = cKey }
                                    ) {
                                        Column {
                                            Text(
                                                cLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) LfEmerald900 else LfSlate800
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                cDesc,
                                                fontSize = 9.sp,
                                                color = if (isSelected) LfEmerald700 else LfSlate400,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Auto-open target app
                        BentoCard(
                            backgroundColor = LfSlate50,
                            borderColor = LfSlate200,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("自动打开目标应用", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                    Text(
                                        "运行任务时，如果目标应用未打开，自动进入该应用",
                                        fontSize = 11.sp,
                                        color = LfSlate500,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Switch(
                                    checked = autoOpenApp,
                                    onCheckedChange = { autoOpenApp = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = LfEmerald500,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = LfSlate300
                                    )
                                )
                            }
                        }

                        Divider(color = LfSlate100)

                        // Next button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            BentoButton(
                                text = "下一步：运行方式 ›",
                                onClick = { currentWizardStep = 2 },
                                backgroundColor = LfEmerald600,
                                modifier = Modifier.height(42.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 2: 运行方式与调度
        // ==========================================
        if (currentWizardStep == 2) {
            item {
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("什么时候运行此任务？", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate800)

                        // 4 Schedule choice cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(ScheduleType.DAILY, "每天运行", "在指定时间段自动执行"),
                                Triple(ScheduleType.WEEKLY, "每周运行", "指定星期几执行"),
                                Triple(ScheduleType.LOOP, "循环运行", "间隔固定分钟重复"),
                                Triple(ScheduleType.MANUAL, "手动运行", "仅在主动点击时执行")
                            ).forEach { (st, label, desc) ->
                                val isSelected = scheduleType == st
                                BentoCard(
                                    backgroundColor = if (isSelected) LfEmerald50 else LfSlate50,
                                    borderColor = if (isSelected) LfEmerald500 else LfSlate200,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(10.dp),
                                    onClick = { scheduleType = st }
                                ) {
                                    Column {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) LfEmerald900 else LfSlate800
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            desc,
                                            fontSize = 9.sp,
                                            color = if (isSelected) LfEmerald700 else LfSlate400,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Daily Config
                        if (scheduleType == ScheduleType.DAILY) {
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("每天运行时间段", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("开始时间", fontSize = 11.sp, color = LfSlate500)
                                            Spacer(Modifier.height(4.dp))
                                            LfInputField(
                                                value = dailyStart,
                                                onValueChange = { dailyStart = it },
                                                placeholder = "09:00",
                                                mono = true,
                                                bold = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("结束时间", fontSize = 11.sp, color = LfSlate500)
                                            Spacer(Modifier.height(4.dp))
                                            LfInputField(
                                                value = dailyEnd,
                                                onValueChange = { dailyEnd = it },
                                                placeholder = "23:00",
                                                mono = true,
                                                bold = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Column {
                                        Text("每天最多执行次数", fontSize = 11.sp, color = LfSlate500)
                                        Spacer(Modifier.height(4.dp))
                                        LfIntField(
                                            value = maxRunsPerDay,
                                            onValueChange = { maxRunsPerDay = it },
                                            min = 1,
                                            max = 50,
                                            modifier = Modifier.width(120.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Weekly Config
                        if (scheduleType == ScheduleType.WEEKLY) {
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("选择执行周期 (可多选)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            1 to "周一",
                                            2 to "周二",
                                            3 to "周三",
                                            4 to "周四",
                                            5 to "周五",
                                            6 to "周六",
                                            7 to "周日"
                                        ).forEach { (dayNum, dayLabel) ->
                                            val isSel = weeklyDays.contains(dayNum)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSel) LfEmerald600 else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) LfEmerald600 else LfSlate200),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        weeklyDays = if (isSel) weeklyDays - dayNum else weeklyDays + dayNum
                                                    }
                                            ) {
                                                Text(
                                                    text = dayLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else LfSlate700,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Loop Config
                        if (scheduleType == ScheduleType.LOOP) {
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("循环间隔设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("每隔", fontSize = 12.sp, color = LfSlate600)
                                        LfIntField(
                                            value = loopIntervalMins,
                                            onValueChange = { loopIntervalMins = it },
                                            min = 1,
                                            max = 720,
                                            modifier = Modifier.width(90.dp)
                                        )
                                        Text("分钟执行一次", fontSize = 12.sp, color = LfSlate600)
                                    }
                                }
                            }
                        }

                        Divider(color = LfSlate100)

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoOutlinedButton(
                                text = "‹ 上一步",
                                onClick = { currentWizardStep = 1 }
                            )
                            BentoButton(
                                text = "下一步：步骤编排 ›",
                                onClick = { currentWizardStep = 3 },
                                backgroundColor = LfEmerald600
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 3: 步骤编排 (Master-Detail Pipeline)
        // ==========================================
        if (currentWizardStep == 3) {
            // Left/Top Step List
            item {
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🗂️", fontSize = 16.sp)
                                    Text("编排步骤列表 (${steps.size})", fontSize = 15.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                                }
                                Text("点击选中步骤，并在下方配置具体动作参数", fontSize = 11.sp, color = LfSlate500)
                            }

                            BentoButton(
                                text = "+ 添加步骤",
                                onClick = { isAddStepMenuOpen = true },
                                backgroundColor = LfEmerald600,
                                modifier = Modifier.height(34.dp)
                            )
                        }

                        // Steps List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            steps.forEachIndexed { idx, step ->
                                val isSelected = step.uid == selectedStepUid
                                BentoCard(
                                    backgroundColor = if (isSelected) LfEmerald50.copy(alpha = 0.85f) else LfSlate50,
                                    borderColor = if (isSelected) LfEmerald500 else LfSlate200,
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(10.dp),
                                    onClick = { selectedStepUid = step.uid }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                String.format("%02d", idx + 1),
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) LfEmerald700 else LfSlate400
                                            )
                                            Column {
                                                Text(
                                                    step.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) LfEmerald900 else LfSlate900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    step.subtitle(),
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) LfEmerald800 else LfSlate500,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { handleDuplicateStep(step.uid) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("📋", fontSize = 12.sp)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { handleDeleteStep(step.uid) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🗑️", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Dashed Add button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    androidx.compose.ui.graphics.SolidColor(LfEmerald400),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { isAddStepMenuOpen = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ 添加新步骤", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfEmerald700)
                        }
                    }
                }
            }

            // Right/Bottom Step Detail Config Form
            item {
                val activeStepIndex = steps.indexOfFirst { it.uid == selectedStepUid }.coerceAtLeast(0)
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "当前步骤配置 · #${activeStepIndex + 1} ${activeStep.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfSlate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            StepKindBadge(activeStep.kind)
                        }

                        Divider(color = LfSlate100)

                        // The StepConfigForms
                        StepConfigForms(
                            step = activeStep,
                            onChange = handleUpdateStep,
                            onOpenCoordPicker = { isCoordPickerOpen = true },
                            onOpenAreaPicker = { isAreaPickerOpen = true },
                            onOpenOcrPicker = { isOcrPickerOpen = true },
                            onOpenPageConditionPicker = { isPageConditionOpen = true },
                            subtaskCandidates = allTasks
                        )

                        Divider(color = LfSlate100)

                        // Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoOutlinedButton(
                                text = "‹ 上一步",
                                onClick = { currentWizardStep = 2 }
                            )
                            BentoButton(
                                text = "下一步：检查并保存 ›",
                                onClick = { currentWizardStep = 4 },
                                backgroundColor = LfEmerald600
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 4: 检查并保存
        // ==========================================
        if (currentWizardStep == 4) {
            item {
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LfEmerald100),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LfEmerald700)
                            }
                            Column {
                                Text("检查任务配置摘要", fontSize = 16.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                                Text("确认无误后即可保存并在指定时间或立即执行", fontSize = 11.sp, color = LfSlate500)
                            }
                        }

                        // Summary Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column {
                                    Text("任务名称", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                    Text(taskName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column {
                                    Text("目标应用", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                    Text("${selectedApp.icon} ${selectedApp.appName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column {
                                    Text("运行触发", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                    Text(
                                        when (scheduleType) {
                                            ScheduleType.DAILY -> "每天 $dailyStart ~ $dailyEnd"
                                            ScheduleType.WEEKLY -> "每周计划"
                                            ScheduleType.LOOP -> "每 $loopIntervalMins 分钟"
                                            else -> "手动运行"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LfSlate900
                                    )
                                }
                            }
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column {
                                    Text("步骤总计", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                    Text("${steps.size} 步骤 · 约 ${Math.ceil((steps.size * 15 + 20) / 60.0).toInt()} 分钟", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                }
                            }
                        }

                        // Steps Execution Flow Overview
                        BentoCard(
                            backgroundColor = LfSlate50,
                            borderColor = LfSlate200,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("步骤执行流快速概览:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSlate700)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    steps.forEachIndexed { idx, s ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LfSlate200)
                                        ) {
                                            Text(
                                                "${idx + 1}. ${s.name}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = LfSlate800,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (idx < steps.size - 1) {
                                            Text("→", fontSize = 10.sp, color = LfSlate300)
                                        }
                                    }
                                }
                            }
                        }

                        // AI Optimization Tip Banner
                        BentoCard(
                            backgroundColor = LfTeal50,
                            borderColor = LfTeal200,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("✨", fontSize = 16.sp)
                                Column {
                                    Text("编排建议与风险检测", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfTeal900)
                                    Text(
                                        "当前任务均采用文字视觉识别，适配性优秀。已自动为每个等待步骤配置了 10~15 秒超时防死锁保护。",
                                        fontSize = 11.sp,
                                        color = LfTeal900,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = LfSlate100)

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoOutlinedButton(
                                text = "‹ 返回修改步骤",
                                onClick = { currentWizardStep = 3 }
                            )
                            BentoButton(
                                text = "✓ 保存自动化任务",
                                onClick = handleFinalSave,
                                backgroundColor = LfEmerald600,
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Picker Modals
    AppSelectModal(
        isOpen = isAppSelectOpen,
        apps = availableApps,
        selectedPackage = selectedApp.packageName,
        onSelectApp = { selectedApp = it },
        onClose = { isAppSelectOpen = false }
    )

    AddStepMenuModal(
        isOpen = isAddStepMenuOpen,
        onClose = { isAddStepMenuOpen = false },
        onSelectStepType = handleAddStep
    )

    CoordinatePickerModal(
        isOpen = isCoordPickerOpen,
        initialX = activeStep.x,
        initialY = activeStep.y,
        onConfirm = { nx, ny -> handleUpdateStep(activeStep.copy(x = nx, y = ny)) },
        onClose = { isCoordPickerOpen = false }
    )

    AreaPickerModal(
        isOpen = isAreaPickerOpen,
        initialRegion = activeStep.targetRegion,
        onConfirm = { reg -> handleUpdateStep(activeStep.copy(targetRegion = reg)) },
        onClose = { isAreaPickerOpen = false }
    )

    OcrPickerModal(
        isOpen = isOcrPickerOpen,
        ocrResult = ocrResult,
        onRescan = onRunOcrDebug,
        onConfirm = { word ->
            handleUpdateStep(
                activeStep.copy(
                    targetText = word,
                    name = if (activeStep.kind == WizardStepKind.CLICK_TEXT) "点击“$word”" else "等待“$word”出现"
                )
            )
        },
        onClose = { isOcrPickerOpen = false }
    )

    PageConditionModal(
        isOpen = isPageConditionOpen,
        initialMustAppear = activeStep.mustAppearTexts,
        initialMustNotAppear = activeStep.mustNotAppearTexts,
        onConfirm = { appear, notAppear ->
            handleUpdateStep(
                activeStep.copy(
                    mustAppearTexts = appear,
                    mustNotAppearTexts = notAppear
                )
            )
        },
        onClose = { isPageConditionOpen = false }
    )
}
