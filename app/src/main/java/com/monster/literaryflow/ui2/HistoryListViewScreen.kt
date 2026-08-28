package com.monster.literaryflow.ui2

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.data.db2.RunRecordEntity2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryListViewScreen(
    document: AutomationDocument,
    records: List<RunRecordEntity2>,
    onSelectRecord: (RunRecordEntity2) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateToTasks: () -> Unit
) {
    var activeFilter by remember { mutableStateOf("all") } // all, success, failure, cancelled
    var confirmClear by remember { mutableStateOf(false) }

    val filteredRecords = records.filter { record ->
        when (activeFilter) {
            "success" -> record.status == "COMPLETED"
            "failure" -> record.status == "FAILED"
            "cancelled" -> record.status == "CANCELLED"
            else -> true
        }
    }

    val totalRuns = records.size
    val successRuns = records.count { it.status == "COMPLETED" }
    val failRuns = records.count { it.status == "FAILED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⏱️", fontSize = 20.sp)
                            Text("自动化运行日志", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                        }
                        Text("追踪每次自动化任务的执行耗时、状态与错误堆栈", fontSize = 11.sp, color = LfSlate500, modifier = Modifier.padding(top = 2.dp))
                    }

                    if (records.isNotEmpty()) {
                        BentoOutlinedButton(
                            text = "清空历史",
                            onClick = { confirmClear = true },
                            borderColor = LfRose200,
                            contentColor = LfRose700,
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }
        }

        // 2. Metrics Bento Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text("总执行轮次", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                        Text("$totalRuns 次", fontSize = 16.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                    }
                }
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text("顺利完成", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                        Text("$successRuns 次", fontSize = 16.sp, fontWeight = FontWeight.Black, color = LfEmerald600)
                    }
                }
                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text("异常中断", fontSize = 10.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                        Text("$failRuns 次", fontSize = 16.sp, fontWeight = FontWeight.Black, color = LfRose600)
                    }
                }
            }
        }

        // 3. Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "all" to "全部历史 ($totalRuns)",
                    "success" to "✓ 成功 ($successRuns)",
                    "failure" to "⚠️ 失败 ($failRuns)",
                    "cancelled" to "已取消"
                ).forEach { (fKey, fLabel) ->
                    val isSel = activeFilter == fKey
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isSel) LfEmerald500 else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) LfEmerald500 else LfSlate200),
                        modifier = Modifier.clickable { activeFilter = fKey }
                    ) {
                        Text(
                            text = fLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else LfSlate600,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 4. Record Items
        if (filteredRecords.isEmpty()) {
            item {
                BentoEmptyState(
                    title = "暂无运行历史记录",
                    description = "执行任务后将在此记录每次任务的耗时与分步日志",
                    actionText = "去任务库运行任务",
                    onAction = onNavigateToTasks,
                    iconEmoji = "📜"
                )
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                val task = document.tasks.firstOrNull { it.id == record.taskId }
                val isSuccess = record.status == "COMPLETED"
                val isFailed = record.status == "FAILED"
                val durationSec = if (record.endedAt != null) ((record.endedAt - record.startedAt) / 1000L).coerceAtLeast(1L) else 0L

                BentoCard(
                    backgroundColor = Color.White,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(16.dp),
                    onClick = { onSelectRecord(record) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSuccess) LfEmerald100 else if (isFailed) LfRose100 else LfSlate100),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isSuccess) "✓" else if (isFailed) "!" else "⏹",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccess) LfEmerald700 else if (isFailed) LfRose600 else LfSlate600
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                task?.title ?: "自动化任务 #${record.taskId}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfSlate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.startedAt))} · 耗时 $durationSec 秒",
                                fontSize = 11.sp,
                                color = LfSlate500
                            )
                            if (record.message != null) {
                                Text(
                                    record.message,
                                    fontSize = 11.sp,
                                    color = if (isSuccess) LfEmerald800 else LfRose700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        BentoBadge(
                            if (isSuccess) "执行成功" else if (isFailed) "异常失败" else "已取消",
                            if (isSuccess) LfEmerald50 else if (isFailed) LfRose50 else LfSlate100,
                            if (isSuccess) LfEmerald700 else if (isFailed) LfRose700 else LfSlate700,
                            if (isSuccess) LfEmerald200 else if (isFailed) LfRose200 else LfSlate300
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        BentoConfirmDialog(
            isOpen = true,
            title = "清空所有运行历史记录？",
            description = "清空后所有历史执行耗时、成功率统计与错误日志将无法找回。",
            isDestructive = true,
            confirmText = "清空历史",
            onConfirm = {
                confirmClear = false
                onClearHistory()
            },
            onDismiss = { confirmClear = false }
        )
    }
}
