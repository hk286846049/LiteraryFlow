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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.TaskModel
import com.monster.literaryflow.data.db2.RunRecordEntity2
import com.monster.literaryflow.data.db2.StepRecordEntity2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// 1. Run Success View Screen (执行成功页)
// =========================================================================
@Composable
fun RunSuccessViewScreen(
    task: TaskModel,
    durationSec: Int = 42,
    onRerun: () -> Unit,
    onViewDetails: () -> Unit,
    onBackToTask: () -> Unit
) {
    val iconEmoji = getAppCategoryIcon(task.category, task.targetAppName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Big Success Badge with sparkling indicator
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(LfEmerald100),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 48.sp, fontWeight = FontWeight.Black, color = LfEmerald600)
            }
            Text("✨", fontSize = 24.sp, modifier = Modifier.align(Alignment.TopEnd))
        }

        Spacer(Modifier.height(18.dp))
        Text("自动化任务执行完成", fontSize = 22.sp, fontWeight = FontWeight.Black, color = LfSlate900)
        Text("所有预定编排步骤均已按预期顺序顺利执行", fontSize = 12.sp, color = LfSlate500, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(20.dp))

        // Summary Card
        BentoCard(
            backgroundColor = Color.White,
            borderColor = LfSlate200,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LfSlate50)
                            .border(1.dp, LfSlate200, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(iconEmoji, fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                        Text(task.targetAppName, fontSize = 11.sp, color = LfSlate500)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Divider(color = LfSlate100)
                Spacer(Modifier.height(12.dp))

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
                            Text("完成步骤", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                            Text("${task.steps.size} / ${task.steps.size} 步", fontSize = 14.sp, fontWeight = FontWeight.Black, color = LfEmerald700)
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
                            Text("实际耗时", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                            Text("${durationSec / 60} 分 ${durationSec % 60} 秒", fontSize = 14.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action Buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BentoButton(
                text = "再次运行该任务",
                onClick = onRerun,
                backgroundColor = LfEmerald500,
                modifier = Modifier.fillMaxWidth().height(46.dp)
            )

            BentoOutlinedButton(
                text = "查看运行详情与日志",
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )

            TextButton(
                onClick = onBackToTask,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回任务主页", fontSize = 12.sp, color = LfSlate500)
            }
        }
    }
}

// =========================================================================
// 2. Run Failure View Screen (执行异常/失败定位页)
// =========================================================================
@Composable
fun RunFailureViewScreen(
    task: TaskModel,
    failedStepIndex: Int = 4,
    failureReason: String = "在第 4 步寻找“领取”超时未响应 (尝试 3/3 次)",
    onRerun: () -> Unit,
    onEditTask: () -> Unit,
    onViewDetails: () -> Unit,
    onBackToTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Red Triangle Alert Badge
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(LfRose100),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠️", fontSize = 42.sp)
        }

        Spacer(Modifier.height(18.dp))
        Text("任务未完全完成", fontSize = 22.sp, fontWeight = FontWeight.Black, color = LfSlate900)
        Text("在执行过程中遇到页面未匹配或识别超时", fontSize = 12.sp, color = LfSlate500, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(20.dp))

        // Failure Reason Card
        BentoCard(
            backgroundColor = Color.White,
            borderColor = LfRose200,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(task.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                    BentoBadge("中断在第 $failedStepIndex 步", LfRose50, LfRose700, LfRose200)
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LfRose50)
                        .border(1.dp, LfRose200, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("失败原因分析", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfRose900)
                        Spacer(Modifier.height(4.dp))
                        Text(failureReason, fontSize = 12.sp, color = LfRose700, lineHeight = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "💡 建议：可尝试将“完全匹配”改为“模糊匹配”，或延长该步骤的超时等待时间。",
                    fontSize = 10.sp,
                    color = LfSlate400,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action Buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BentoButton(
                text = "重新运行该任务",
                onClick = onRerun,
                backgroundColor = LfEmerald500,
                modifier = Modifier.fillMaxWidth().height(46.dp)
            )

            BentoButton(
                text = "✏️ 编辑并调整此步骤",
                onClick = onEditTask,
                backgroundColor = LfSlate800,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )

            BentoOutlinedButton(
                text = "查看执行时间线详情",
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            )

            TextButton(
                onClick = onBackToTask,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回任务主页", fontSize = 12.sp, color = LfSlate500)
            }
        }
    }
}

// =========================================================================
// 3. Run Record Detail Timeline Screen (运行历史步骤日志详情)
// =========================================================================
@Composable
fun RunRecordDetailTimelineScreen(
    document: AutomationDocument,
    records: List<RunRecordEntity2>,
    stepRecords: List<StepRecordEntity2>,
    recordId: Long?,
    onBack: () -> Unit,
    onRerun: () -> Unit
) {
    val record = records.firstOrNull { it.id == recordId }
    val task = document.tasks.firstOrNull { it.id == record?.taskId }
    val isSuccess = record?.status == "COMPLETED"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navigation Header
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
                    BentoOutlinedButton(text = "‹ 返回日志列表", onClick = onBack, modifier = Modifier.height(36.dp))
                    BentoButton(text = "再次运行", onClick = onRerun, modifier = Modifier.height(36.dp))
                }
            }
        }

        // Summary Card
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task?.title ?: "自动化任务 #${record?.taskId ?: "-"}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                        BentoBadge(
                            if (isSuccess) "顺利完成" else "执行失败",
                            if (isSuccess) LfEmerald50 else LfRose50,
                            if (isSuccess) LfEmerald700 else LfRose700,
                            if (isSuccess) LfEmerald200 else LfRose200
                        )
                    }

                    Divider(color = LfSlate100)

                    Text("开始时间: ${record?.startedAt?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "-"}", fontSize = 11.sp, color = LfSlate500)
                    Text("结束时间: ${record?.endedAt?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "进行中"}", fontSize = 11.sp, color = LfSlate500)
                    if (record?.message != null) {
                        Text("日志备注: ${record.message}", fontSize = 11.sp, color = if (isSuccess) LfEmerald800 else LfRose700, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Step Execution Timeline
        item {
            SectionHeader("步骤执行时间轴日志", "按执行顺序追踪每个步骤的 OCR 识别与模拟事件")
        }

        if (task?.steps.isNullOrEmpty()) {
            item {
                BentoEmptyState("暂无步骤日志", "未捕获到分步执行记录", iconEmoji = "📜")
            }
        } else {
            itemsIndexed(task?.steps.orEmpty()) { idx, step ->
                val sRec = stepRecords.firstOrNull { it.stepId == step.id }
                val stepStatus = sRec?.status ?: if (isSuccess) "COMPLETED" else if (idx == 3) "FAILED" else "COMPLETED"

                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (stepStatus == "COMPLETED") LfEmerald100 else LfRose100),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (stepStatus == "COMPLETED") "✓" else "!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stepStatus == "COMPLETED") LfEmerald800 else LfRose600
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("${idx + 1}. ${step.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                            Text(
                                sRec?.message ?: if (stepStatus == "COMPLETED") "步骤执行完毕并匹配成功" else "等待目标出现超时",
                                fontSize = 11.sp,
                                color = LfSlate500
                            )
                        }

                        Text(
                            if (stepStatus == "COMPLETED") "成功" else "失败",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stepStatus == "COMPLETED") LfEmerald700 else LfRose600
                        )
                    }
                }
            }
        }
    }
}
