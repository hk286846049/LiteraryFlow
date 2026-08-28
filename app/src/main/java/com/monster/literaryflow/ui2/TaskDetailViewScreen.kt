package com.monster.literaryflow.ui2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.data.db2.RunRecordEntity2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskDetailViewScreen(
    task: TaskModel,
    records: List<RunRecordEntity2>,
    onBack: () -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onEditTask: (TaskModel) -> Unit,
    onEditSteps: (TaskModel) -> Unit = onEditTask,
    onToggleEnable: (TaskModel) -> Unit,
    onToggleFavorite: (TaskModel) -> Unit,
    onDuplicateTask: (TaskModel) -> Unit,
    onDeleteTaskRequest: (TaskModel) -> Unit,
    onDeleteStep: (TaskModel, StepModel) -> Unit,
    onSelectRecord: (Long) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val taskRecords = remember(records, task.id) {
        records.filter { it.taskId == task.id }
    }
    val iconEmoji = getAppCategoryIcon(task.category, task.targetAppName)

    val lastRecord = taskRecords.firstOrNull()
    val lastRecordTime = if (lastRecord != null) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastRecord.startedAt))
    } else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Navigation & Action Bar
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BentoOutlinedButton(
                        text = "‹ 返回列表",
                        onClick = onBack,
                        modifier = Modifier.height(36.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Toggle Enable/Disable
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (task.enabled) LfSlate100 else LfEmerald50,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (task.enabled) LfSlate200 else LfEmerald200
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .clickable { onToggleEnable(task) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("⚡", fontSize = 12.sp)
                                Text(
                                    if (task.enabled) "停用任务" else "启用任务",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.enabled) LfSlate700 else LfEmerald800
                                )
                            }
                        }

                        // Duplicate
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LfSlate100)
                                .clickable { onDuplicateTask(task) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📋", fontSize = 14.sp)
                        }

                        // Edit
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LfSlate100,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable { onEditTask(task) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("✏️", fontSize = 12.sp)
                                Text("编辑", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                            }
                        }

                        // Delete
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LfRose50)
                                .clickable { confirmDelete = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 2. Hero Header Card
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(LfSlate50)
                                    .border(1.dp, LfSlate200, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(iconEmoji, fontSize = 28.sp)
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        task.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = LfSlate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    BentoBadge(
                                        if (task.enabled) "已启用" else "已停用",
                                        if (task.enabled) LfEmerald50 else LfSlate100,
                                        if (task.enabled) LfEmerald700 else LfSlate600,
                                        if (task.enabled) LfEmerald200 else LfSlate300
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${task.targetAppName.ifBlank { "未指定应用" }} • ${task.targetPackageName}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = LfSlate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Star Favorite
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { onToggleFavorite(task) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (task.favorite) "★" else "☆",
                                fontSize = 20.sp,
                                color = if (task.favorite) Color(0xFFF59E0B) else LfSlate300
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Large Run Button
                    BentoButton(
                        text = "▶  立即执行任务",
                        onClick = { onRunTask(task) },
                        backgroundColor = LfEmerald600,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider(color = LfSlate100)
                    Spacer(Modifier.height(14.dp))

                    // 4-Column Overview Meta Grid
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
                                Text("运行触发方式", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                Text(getScheduleDisplay(task), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSlate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                Text("今日运行情况", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                Text("已运行 ${task.todayRunCount} 次", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfEmerald800)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

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
                                Text("上次执行结果", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                if (lastRecord != null) {
                                    val isSuccess = lastRecord.status == "COMPLETED"
                                    Text(
                                        if (isSuccess) "成功 ($lastRecordTime)" else "失败 ($lastRecordTime)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSuccess) LfEmerald700 else LfRose600
                                    )
                                } else {
                                    Text("尚无运行记录", fontSize = 11.sp, color = LfSlate500)
                                }
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
                                Text("预估平均耗时", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                Text(
                                    "约 ${(task.estimatedDurationMs / 60000L).coerceAtLeast(1L)} 分钟 (${task.steps.size} 步骤)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LfSlate900
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Task Execution Steps Flow (with Timeline Connector)
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                Text(
                                    "任务执行步骤流 (${task.steps.size} 个步骤)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = LfSlate900
                                )
                            }
                            Text("按从上至下的时间线顺序执行", fontSize = 11.sp, color = LfSlate500)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LfEmerald50,
                            modifier = Modifier.clickable { onEditSteps(task) }
                        ) {
                            Text(
                                "编辑步骤 ›",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfEmerald800,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (task.steps.isEmpty()) {
                        BentoEmptyState(
                            title = "任务还没有编排步骤",
                            description = "点击上方“编辑步骤”，添加文字识别、坐标点击或等待动作",
                            actionText = "去添加步骤",
                            onAction = { onEditSteps(task) },
                            iconEmoji = "🧩"
                        )
                    } else {
                        // Steps timeline list
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            task.steps.forEachIndexed { index, step ->
                                val action = step.actions.firstOrNull()
                                val stepIcon = when (action?.type) {
                                    ActionType.TAP_TEXT, ActionType.TAP_COORDINATE -> "👆"
                                    ActionType.INPUT_TEXT -> "⌨️"
                                    ActionType.WAIT_FOR_TEXT, ActionType.WAIT_FOR_SCREEN, ActionType.WAIT -> "⏳"
                                    ActionType.OPEN_APP -> "📱"
                                    ActionType.BACK -> "↩️"
                                    ActionType.SWIPE -> "↔️"
                                    ActionType.LONG_PRESS -> "⏱️"
                                    ActionType.RUN_SUBTASK -> "🔗"
                                    else -> "⚡"
                                }

                                val stepTag = when (action?.type) {
                                    ActionType.TAP_TEXT -> "文字: “${action.text ?: ""}”"
                                    ActionType.WAIT_FOR_TEXT -> "等待: “${action.text ?: ""}” (${step.timeoutMs / 1000}s)"
                                    ActionType.WAIT_FOR_SCREEN -> "页面特征"
                                    ActionType.TAP_COORDINATE -> "坐标 (${action.x ?: 0.5f}, ${action.y ?: 0.5f})"
                                    ActionType.SWIPE -> "滑动 (${if ((action.swipe?.startY ?: 0f) > (action.swipe?.endY ?: 0f)) "向上" else "向下滑动"})"
                                    ActionType.OPEN_APP -> "启动: ${action.targetAppName ?: action.targetPackageName ?: "应用"}"
                                    ActionType.WAIT -> "等待 ${step.delayAfterMs / 1000} 秒"
                                    ActionType.BACK -> "系统返回"
                                    ActionType.INPUT_TEXT -> "输入: “${action.inputText ?: action.text ?: ""}”"
                                    else -> getStepTypeDescription(step)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Step Index Circle
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(2.dp, LfEmerald500, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LfEmerald700
                                        )
                                    }

                                    // Step Card
                                    BentoCard(
                                        backgroundColor = LfSlate50,
                                        borderColor = LfSlate200,
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stepIcon, fontSize = 13.sp)
                                                    Text(
                                                        step.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LfSlate900,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, LfSlate200)
                                                ) {
                                                    Text(
                                                        stepTag,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = LfSlate600,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (!step.description.isNullOrBlank()) {
                                                Text(
                                                    step.description!!,
                                                    fontSize = 11.sp,
                                                    color = LfSlate500,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Task Run History Section
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⏱️", fontSize = 16.sp)
                        Text(
                            "该任务的历史运行记录 (${taskRecords.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = LfSlate900
                        )
                    }

                    if (taskRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无此任务的执行历史", fontSize = 12.sp, color = LfSlate400)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            taskRecords.take(8).forEach { item ->
                                val isSuccess = item.status == "COMPLETED"
                                val durationSec = if (item.endedAt != null) {
                                    ((item.endedAt - item.startedAt) / 1000L).coerceAtLeast(1L)
                                } else 0L
                                val timeStr = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.startedAt))

                                BentoCard(
                                    backgroundColor = LfSlate50,
                                    borderColor = LfSlate200,
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(12.dp),
                                    onClick = { onSelectRecord(item.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(timeStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate800)
                                            Text("•", fontSize = 10.sp, color = LfSlate400)
                                            Text("耗时 ${durationSec}s", fontSize = 11.sp, color = LfSlate500)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            BentoBadge(
                                                if (isSuccess) "成功" else "失败",
                                                if (isSuccess) LfEmerald50 else LfRose50,
                                                if (isSuccess) LfEmerald700 else LfRose600,
                                                if (isSuccess) LfEmerald200 else LfRose200
                                            )
                                            Text("›", fontSize = 14.sp, color = LfSlate400)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (confirmDelete) {
        BentoConfirmDialog(
            isOpen = true,
            title = "删除任务 “${task.title}”？",
            description = "删除后此任务的步骤配置将无法恢复，是否确认删除？",
            isDestructive = true,
            confirmText = "确认删除",
            onConfirm = {
                confirmDelete = false
                onDeleteTaskRequest(task)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
