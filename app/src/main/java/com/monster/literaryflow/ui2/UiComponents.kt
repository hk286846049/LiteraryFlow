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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.TaskModel

// ==========================================
// Bento Card Container
// ==========================================
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LfCardBg,
    borderColor: Color = LfBorder,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

// ==========================================
// Status Badges & Chips
// ==========================================
@Composable
fun BentoBadge(
    text: String,
    backgroundColor: Color = LfEmerald50,
    textColor: Color = LfEmerald700,
    borderColor: Color = LfEmerald200,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PulseDot(
    color: Color = LfEmerald500,
    size: Dp = 8.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

// ==========================================
// Primary Action Button (Emerald Rounded Pill)
// ==========================================
@Composable
fun BentoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LfEmerald500,
    contentColor: Color = Color.White,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = LfSlate200,
            disabledContentColor = LfSlate400
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BentoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = LfBorder,
    contentColor: Color = LfSlate700,
    icon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================
// Empty State View
// ==========================================
@Composable
fun BentoEmptyState(
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    iconEmoji: String = "📦",
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color.White,
        borderColor = LfSlate200
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = iconEmoji,
                fontSize = 42.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LfSlate900,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = LfSlate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                BentoButton(
                    text = actionText,
                    onClick = onAction
                )
            }
        }
    }
}

// ==========================================
// Section Header
// ==========================================
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LfSlate900
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = LfSlate500
                )
            }
        }
        if (actionText != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    color = LfEmerald600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// Helper Formatters
// ==========================================
fun getScheduleDisplay(task: TaskModel): String {
    return when (task.scheduleType) {
        ScheduleType.DAILY -> {
            val s = task.startTime ?: "09:00"
            val e = task.endTime ?: "23:00"
            "每日 $s ~ $e · 每日 ${task.runTimes.coerceAtLeast(1)} 次"
        }
        ScheduleType.WEEKLY -> {
            val days = task.weeklyDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: listOf(1,2,3,4,5,6,7)
            val dayStr = days.joinToString("、") { d ->
                when(d) {
                    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; else -> "周日"
                }
            }
            "每周 ($dayStr)"
        }
        ScheduleType.LOOP -> {
            val mins = (task.intervalMs / 60000L).coerceAtLeast(1L)
            "循环 · 每 $mins 分钟"
        }
        ScheduleType.RUN_COUNT -> "固定运行 ${task.runTimes} 次"
        ScheduleType.MANUAL -> "手动触发执行"
    }
}

fun getAppCategoryIcon(category: String, appName: String): String {
    return when {
        category.contains("game", true) || appName.contains("舟") || appName.contains("铁") || appName.contains("原神") -> "🏰"
        category.contains("ad", true) || appName.contains("广告") || appName.contains("宝箱") -> "🎁"
        appName.contains("短视频") || appName.contains("快手") || appName.contains("抖音") -> "🎬"
        appName.contains("王者") -> "⚔️"
        else -> "⚡"
    }
}

fun getStepTypeDescription(step: StepModel): String {
    val act = step.actions.firstOrNull()
    val cond = step.conditions.firstOrNull()
    return when {
        act?.type == ActionType.TAP_TEXT -> "识别屏幕文字“${act.text}”并点击"
        act?.type == ActionType.WAIT_FOR_TEXT -> "持续识别，等待“${act.text}”出现"
        act?.type == ActionType.WAIT_FOR_SCREEN -> "多条件判断：必须包含“${act.text}”"
        act?.type == ActionType.TAP_COORDINATE -> "相对坐标点击 (X: ${((act.x ?: 0.5f)*100).toInt()}%, Y: ${((act.y ?: 0.5f)*100).toInt()}%)"
        act?.type == ActionType.SWIPE -> "屏幕滑动"
        act?.type == ActionType.LONG_PRESS -> "长按指定位置"
        act?.type == ActionType.INPUT_TEXT -> "填入文字：“${act.inputText}”"
        act?.type == ActionType.WAIT -> "固定等待 ${act.timeoutMs / 1000} 秒"
        act?.type == ActionType.BACK -> "模拟按下系统返回键"
        act?.type == ActionType.OPEN_APP -> "拉起目标前台应用"
        act?.type == ActionType.RUN_SUBTASK -> "调用子任务 #${act.childTaskId}"
        cond != null -> "等待满足页面感知特征条件"
        else -> "自动化编排指令"
    }
}
