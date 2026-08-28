package com.monster.literaryflow.ui2

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay

// =========================================================================
// 1. Home View Screen (首页 Bento 仪表盘)
// =========================================================================
@Composable
fun HomeScreenView(
    document: AutomationDocument,
    runState: RunState,
    runRecords: List<RunRecordEntity2>,
    loading: Boolean,
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    screenCaptureGranted: Boolean,
    batteryOptimizationGranted: Boolean,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onCancelRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenTemplates: () -> Unit,
    onCreateTask: () -> Unit,
    onSelectTask: (Long) -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllHistory: () -> Unit
) {
    var previewState by remember { mutableStateOf("normal") } // normal, running, done, alert
    var confirmHomeStop by remember { mutableStateOf(false) }

    val tasks = document.tasks
    val totalTasks = tasks.size
    val enabledTasks = tasks.count { it.enabled }
    val waitingTasks = tasks.count { it.enabled && it.todayRunCount == 0 }
    val completedTodayCount = tasks.count { it.todayRunCount > 0 }
    val completionPercent = if (totalTasks == 0) 0 else ((enabledTasks.toFloat() / totalTasks.toFloat()) * 100).toInt()

    val now = Date()
    val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)
    val dateString = dateFormat.format(now)

    val isRunningOrPaused = runState.status == RunStatus.RUNNING || runState.status == RunStatus.PAUSED || previewState == "running"
    val runningTask = tasks.firstOrNull { it.id == runState.taskId } ?: tasks.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Prototype Preview Mode Selector
        item {
            BentoCard(
                backgroundColor = Color.White.copy(alpha = 0.9f),
                borderColor = LfSlate200,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "原型演示:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LfSlate500,
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                    )
                    listOf(
                        "normal" to "待命",
                        "running" to "实时运行 (3/8步)",
                        "done" to "全部完成",
                        "alert" to "异常告警"
                    ).forEach { (modeKey, modeLabel) ->
                        val isSelected = previewState == modeKey
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) LfEmerald500 else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) LfEmerald500 else LfSlate200),
                            modifier = Modifier.clickable { previewState = modeKey }
                        ) {
                            Text(
                                text = modeLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else LfSlate600,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    BentoBadge("✨ 模板库", LfEmerald50, LfEmerald700, LfEmerald200, modifier = Modifier.clickable { onOpenTemplates() })
                }
            }
        }

        // 1. Bento Header Greeting
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulseDot(LfEmerald500, 7.dp)
                    Text(dateString, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfEmerald600)
                    Text("·", fontSize = 11.sp, color = LfEmerald600)
                    Text("视觉自动化守护中", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfEmerald600)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (previewState == "done") "今日任务全部顺利完成！" else "下午好，文流用户",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = LfEmerald800
                        )
                        Text(
                            text = if (previewState == "done")
                                "所有配置任务均已妥善执行，节省了约 28 分钟重复点击。"
                            else
                                "当前 $totalTasks 个任务就绪 · $completedTodayCount 个已完成 · $waitingTasks 个待触发",
                            fontSize = 12.sp,
                            color = LfSlate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LfEmerald500),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 20.sp)
                    }
                }
            }
        }

        // 2. Alert Warning (If alert mode)
        if (previewState == "alert") {
            item {
                BentoCard(
                    backgroundColor = LfAmber100,
                    borderColor = LfAmber200,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LfAmber200),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚠️", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("需要注意：最近 3 次运行存在超时未匹配", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfAmber900)
                                BentoBadge("广告跳过", LfAmber200, LfAmber900, LfAmber400)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "任务“自动检测与跳过全屏广告”在寻找右上角“关闭”图标时超时未响应，可能由于应用更新了浮层布局。",
                                fontSize = 11.sp,
                                color = LfAmber800,
                                lineHeight = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BentoButton(
                                    text = "检查与编辑步骤",
                                    onClick = { tasks.firstOrNull()?.let { onSelectTask(it.id) } },
                                    backgroundColor = Color.White,
                                    contentColor = LfAmber900,
                                    modifier = Modifier.height(32.dp)
                                )
                                BentoOutlinedButton(
                                    text = "查看失败日志",
                                    onClick = onViewAllHistory,
                                    borderColor = LfAmber400,
                                    contentColor = LfAmber900,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Permissions Notification Banner (if unready)
        if (!accessibilityGranted || !overlayGranted || !screenCaptureGranted) {
            item {
                BentoCard(
                    backgroundColor = LfAmber100.copy(alpha = 0.8f),
                    borderColor = LfAmber200,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                            Column {
                                Text("核心运行权限尚未全部开启", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfAmber900)
                                Text("无障碍、悬浮窗或屏幕截屏需就绪后才能自动执行", fontSize = 11.sp, color = LfAmber800)
                            }
                        }
                        BentoButton("去授权", onClick = onOpenPermissions, backgroundColor = LfAmber700, modifier = Modifier.height(34.dp))
                    }
                }
            }
        }

        // 4. Hero Automation Bento Overview Card
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("今日执行总览", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSlate400)
                            Text(
                                if (previewState == "done") "100% 全部完成" else "自动化守护就绪",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = LfSlate900
                            )
                        }
                        BentoBadge("✨ 达成率 ${if (previewState == "done") 100 else 85}%", LfEmerald50, LfEmerald700, LfEmerald200)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress Ring & Stat metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Circular Gauge
                        Box(contentAlignment = Alignment.Center) {
                            val percent = if (previewState == "done") 1f else 0.85f
                            Canvas(modifier = Modifier.size(110.dp)) {
                                drawArc(
                                    color = LfSlate100,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = LfEmerald500,
                                    startAngle = -90f,
                                    sweepAngle = 360f * percent,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (previewState == "done") "100%" else "85%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = LfSlate900
                                )
                                Text("已执行 $completedTodayCount/$totalTasks 任务", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LfSlate400)
                            }
                        }

                        // Right Metrics
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(LfEmerald500))
                                Column {
                                    Text("已节省操作时间", fontSize = 10.sp, color = LfSlate500)
                                    Text("28 mins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(LfSky400))
                                Column {
                                    Text("平均识别响应", fontSize = 10.sp, color = LfSlate500)
                                    Text("18 ms", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(LfSlate300))
                                Column {
                                    Text("历史执行成功率", fontSize = 10.sp, color = LfSlate500)
                                    Text("98.5%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Divider(color = LfSlate100)
                    Spacer(Modifier.height(12.dp))

                    // Sub-tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoCard(
                            backgroundColor = LfSlate50,
                            borderColor = LfSlate200,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Column {
                                Text("定时待触发", fontSize = 10.sp, color = LfSlate500)
                                Text("$waitingTasks 个", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
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
                                Text("总模拟点击量", fontSize = 10.sp, color = LfSlate500)
                                Text("9,240 次", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LfEmerald600)
                            }
                        }
                    }
                }
            }
        }

        // 5. System Perception & Screen Capture Engine Card
        item {
            BentoCard(
                backgroundColor = LfSky500,
                borderColor = LfSky400,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("系统感知与 OCR 引擎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSky100)
                            Text(if (screenCaptureGranted) "投屏 OCR 极速就绪" else "投屏捕获待授权", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text("☀️", fontSize = 24.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("实时感知帧率: 60 FPS", fontSize = 11.sp, color = LfSky100)
                        Text("平均识别延迟: 12 ms", fontSize = 11.sp, color = LfSky100)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoButton(
                            text = if (screenCaptureGranted) "重新捕获" else "开启屏幕感知",
                            onClick = onStartCapture,
                            backgroundColor = Color.White,
                            contentColor = LfSky600,
                            modifier = Modifier.weight(1f).height(38.dp)
                        )
                        if (screenCaptureGranted) {
                            BentoOutlinedButton(
                                text = "停止捕获",
                                onClick = onStopCapture,
                                borderColor = Color.White.copy(alpha = 0.5f),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f).height(38.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Live Runner Status Bar (If running or simulated)
        if (isRunningOrPaused && runningTask != null) {
            item {
                BentoCard(
                    backgroundColor = LfSlate800,
                    borderColor = LfSlate700,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PulseDot(LfEmerald400, 8.dp)
                                Text("自动化任务执行中", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfEmerald300)
                            }
                            Text(
                                "步骤 ${runState.currentStepIndex.coerceAtLeast(3)}/${runState.totalSteps.coerceAtLeast(8)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfSlate400
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            runningTask.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            runState.message ?: "正在寻找“领取奖励”文字 (置信度 98%)",
                            fontSize = 11.sp,
                            color = LfSlate300
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = 0.45f,
                            color = LfEmerald500,
                            trackColor = LfSlate700,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BentoButton(
                                text = if (runState.status == RunStatus.PAUSED) "继续执行" else "暂停任务",
                                onClick = { if (runState.status == RunStatus.PAUSED) onResumeRun() else onPauseRun() },
                                backgroundColor = LfSlate700,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f).height(36.dp)
                            )
                            BentoButton(
                                text = "终止任务",
                                onClick = { confirmHomeStop = true },
                                backgroundColor = LfRose600,
                                modifier = Modifier.weight(1f).height(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. Today Upcoming Tasks Schedule (待办任务日程)
        item {
            BentoCard(
                backgroundColor = LfSlate800,
                borderColor = LfSlate700,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("待办任务日程", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSlate400)
                            Text("今日计划流水线", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        BentoButton(
                            text = "+ 新建",
                            onClick = onCreateTask,
                            backgroundColor = LfSlate700,
                            contentColor = Color.White,
                            modifier = Modifier.height(32.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    if (tasks.isEmpty()) {
                        Text("暂无已配置任务，点击上方新建一个", color = LfSlate400, fontSize = 12.sp)
                    } else {
                        tasks.take(4).forEachIndexed { idx, t ->
                            val colorPill = when (idx % 3) {
                                0 -> LfEmerald500
                                1 -> LfSky400
                                else -> LfAmber400
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTask(t.id) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(colorPill)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        t.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        getScheduleDisplay(t),
                                        fontSize = 10.sp,
                                        color = LfSlate400
                                    )
                                }
                                BentoButton(
                                    text = "运行",
                                    onClick = { onRunTask(t) },
                                    backgroundColor = LfEmerald600,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                            if (idx < tasks.take(4).size - 1) {
                                Divider(color = LfSlate700.copy(alpha = 0.5f))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    BentoOutlinedButton(
                        text = "查看全部任务 ($totalTasks)",
                        onClick = onViewAllTasks,
                        borderColor = LfSlate600,
                        contentColor = LfSlate300,
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    )
                }
            }
        }

        // 8. Recent Execution History
        if (runRecords.isNotEmpty()) {
            item {
                SectionHeader("最近运行日志", "记录每次自动化执行结果与耗时", actionText = "全部历史", onAction = onViewAllHistory)
            }
            items(runRecords.take(3)) { record ->
                val task = tasks.firstOrNull { it.id == record.taskId }
                val isSuccess = record.status == "COMPLETED"
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSuccess) LfEmerald100 else LfRose100),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isSuccess) "✓" else "!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccess) LfEmerald700 else LfRose600
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                task?.title ?: "自动化任务 #${record.taskId}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfSlate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${record.startedAt} · ${if (isSuccess) "执行顺利完成" else (record.message ?: "执行中断")}",
                                fontSize = 10.sp,
                                color = LfSlate500
                            )
                        }
                        BentoBadge(
                            if (isSuccess) "成功" else "失败",
                            if (isSuccess) LfEmerald50 else LfRose50,
                            if (isSuccess) LfEmerald700 else LfRose700,
                            if (isSuccess) LfEmerald200 else LfRose200
                        )
                    }
                }
            }
        }
    }

    // Stop Task Confirmation Dialog
    if (confirmHomeStop) {
        BentoConfirmDialog(
            isOpen = true,
            title = "确认停止当前任务？",
            description = "任务停止后本次执行记录将标记为已取消，已完成步骤已记录。",
            isDestructive = true,
            confirmText = "停止任务",
            onConfirm = {
                confirmHomeStop = false
                onCancelRun()
            },
            onDismiss = { confirmHomeStop = false }
        )
    }
}
