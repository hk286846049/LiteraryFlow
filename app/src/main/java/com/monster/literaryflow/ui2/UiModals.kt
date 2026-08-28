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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.monster.literaryflow.core.model.ActionSpec
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.ConditionSpec
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.OcrResultModel
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.StepModel
import com.monster.literaryflow.core.model.StepType
import com.monster.literaryflow.core.model.SwipeSpec
import com.monster.literaryflow.core.model.TaskModel

// =========================================================================
// 1. Task Template Preset Modal (任务预设模板库)
// =========================================================================
data class TaskTemplatePresetUi(
    val id: String,
    val name: String,
    val category: String,
    val icon: String,
    val description: String,
    val steps: List<String>
)

val PRESET_TEMPLATES = listOf(
    TaskTemplatePresetUi(
        id = "tpl-1",
        name = "手游每日签到与福利领取",
        category = "game",
        icon = "🏰",
        description = "自动拉起游戏 → 识别主页“签到”字样 → 点击领取每日奖励 → 退出大厅",
        steps = listOf("打开目标游戏", "等待出现“签到”", "点击“签到”", "等待“领取”奖励", "点击“领取”", "等待动画", "返回主页")
    ),
    TaskTemplatePresetUi(
        id = "tpl-2",
        name = "全屏广告自动跳过与关闭",
        category = "ad",
        icon = "🛡️",
        description = "全屏检测倒计时 → 寻找右上角“跳过/关闭”文字或图标 → 处理挽留弹窗",
        steps = listOf("等待广告倒计时", "点击“跳过/关闭”", "检测挽留弹窗", "点击“坚持退出”")
    ),
    TaskTemplatePresetUi(
        id = "tpl-3",
        name = "短视频自动领金币宝箱",
        category = "ad",
        icon = "🎬",
        description = "滑动浏览视频流 → 停留观看 → 点击宝箱悬浮挂件 → 领取奖励后返回",
        steps = listOf("向上滑动刷视频", "停留观看 8 秒", "点击“开宝箱”", "等待“领取奖励”", "点击“领取”", "返回视频流")
    ),
    TaskTemplatePresetUi(
        id = "tpl-4",
        name = "通用页面特征与定时点击",
        category = "general",
        icon = "⚡",
        description = "基于必须包含/不能包含的多关键词精确判定页面状态，并模拟连续操作",
        steps = listOf("等待目标页面出现", "点击“确认/下一步”", "固定延时等待 2 秒")
    )
)

@Composable
fun TaskTemplateModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    onApplyTemplate: (TaskTemplatePresetUi) -> Unit
) {
    if (!isOpen) return

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, LfBorder, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            Text("✨", fontSize = 18.sp)
                        }
                        Column {
                            Text("选择任务预设模板", fontSize = 16.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                            Text("一键套用经过验证的自动化编排流程", fontSize = 11.sp, color = LfSlate500)
                        }
                    }
                    TextButton(onClick = onClose) {
                        Text("✕", fontSize = 16.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Divider(color = LfSlate150)
                Spacer(Modifier.height(12.dp))

                // Template List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(PRESET_TEMPLATES) { tpl ->
                        BentoCard(
                            backgroundColor = LfSlate50,
                            borderColor = LfSlate200,
                            shape = RoundedCornerShape(20.dp),
                            onClick = {
                                onApplyTemplate(tpl)
                                onClose()
                            }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(tpl.icon, fontSize = 24.sp)
                                        Column {
                                            Text(tpl.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                            val catTag = when(tpl.category) {
                                                "game" -> "手游日常"
                                                "ad" -> "广告处理"
                                                else -> "通用自动化"
                                            }
                                            BentoBadge(catTag, LfEmerald50, LfEmerald700, LfEmerald200)
                                        }
                                    }
                                    BentoButton(
                                        text = "使用此模板",
                                        onClick = {
                                            onApplyTemplate(tpl)
                                            onClose()
                                        },
                                        backgroundColor = LfEmerald600,
                                        modifier = Modifier.height(34.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(tpl.description, fontSize = 12.sp, color = LfSlate600, lineHeight = 16.sp)
                                Spacer(Modifier.height(10.dp))

                                // Steps preview pills
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(1.dp, LfSlate200, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            "内置编排步骤 (${tpl.steps.size} 个):",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LfSlate400
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            tpl.steps.forEachIndexed { index, stepName ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = LfSlate100,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "${index + 1}. $stepName",
                                                        fontSize = 10.sp,
                                                        color = LfSlate700,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (index < tpl.steps.size - 1) {
                                                    Text("→", fontSize = 10.sp, color = LfSlate300)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    BentoOutlinedButton("取消", onClick = onClose)
                }
            }
        }
    }
}

// =========================================================================
// 2. Add Step Menu Modal (11 种原子自动化动作类型菜单)
// =========================================================================
data class StepTypeItem(
    val kind: WizardStepKind,
    val name: String,
    val desc: String,
    val tag: String? = null,
    val iconEmoji: String,
    val accent: Color = LfSlate500,
    val accentSoft: Color = LfSlate100,
    val accentBorder: Color = LfSlate200
)

val STEP_TYPE_GROUPS = listOf(
    "🎯 视觉识别与文字动作 (推荐优先使用)" to listOf(
        StepTypeItem(
            WizardStepKind.CLICK_TEXT, "点击文字",
            "自动识别屏幕中的指定文字并模拟点击 (如：“签到”、“领取”、“跳过”)",
            "推荐", "🔤", LfEmerald700, LfEmerald50, LfEmerald200
        ),
        StepTypeItem(
            WizardStepKind.WAIT_TEXT, "等待文字出现",
            "持续识别屏幕，直到目标文字加载完毕后继续下一步",
            "常用", "👁️", LfTeal700, LfTeal50, LfTeal200
        ),
        StepTypeItem(
            WizardStepKind.WAIT_PAGE, "等待指定页面",
            "通过组合多个必须出现与不能出现的文字，精确判断当前页面状态",
            "精准", "📑", LfIndigo700, LfIndigo50, LfIndigo200
        )
    ),
    "👆 手势与输入操作" to listOf(
        StepTypeItem(WizardStepKind.CLICK, "点击指定坐标", "在屏幕相对比例坐标 (X, Y) 处触发单次或多次点击", null, "🎯", LfSky600, LfSky50, LfSky200),
        StepTypeItem(WizardStepKind.SWIPE, "屏幕滑动", "向上、向下或自定义轨迹滑动，用于翻页或刷视频", null, "↔️", LfSky600, LfSky50, LfSky200),
        StepTypeItem(WizardStepKind.LONG_PRESS, "长按操作", "在指定位置按住屏幕指定毫秒数 (如长按跳过剧情)", null, "⏱️", LfSky600, LfSky50, LfSky200),
        StepTypeItem(WizardStepKind.INPUT_TEXT, "输入文字", "向当前获得焦点的输入框填入指定的文本内容", null, "⌨️", LfSky600, LfSky50, LfSky200)
    ),
    "⚙️ 流程与系统控制" to listOf(
        StepTypeItem(WizardStepKind.WAIT, "固定等待时间", "单纯延时等待数秒，常用于动画播完或网络过渡", null, "⏳"),
        StepTypeItem(WizardStepKind.BACK, "系统返回键", "模拟按下 Android 返回键，关闭弹窗或退回大厅", null, "↩️"),
        StepTypeItem(WizardStepKind.OPEN_APP, "打开目标应用", "切换或重新启动指定的游戏或工具 App", null, "📱"),
        StepTypeItem(WizardStepKind.SUBTASK, "执行子任务", "调用另一个独立的自动化任务，完成后继续流转", null, "🔗")
    )
)

@Composable
fun AddStepMenuModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSelectStepType: (WizardStepKind) -> Unit
) {
    if (!isOpen) return

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .border(1.dp, LfBorder, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("添加编排步骤", fontSize = 17.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                        Text("选择要执行的自动化动作或感知条件", fontSize = 11.sp, color = LfSlate500)
                    }
                    TextButton(onClick = onClose) {
                        Text("✕", fontSize = 16.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Divider(color = LfSlate150)
                Spacer(Modifier.height(8.dp))

                // Action Groups List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    STEP_TYPE_GROUPS.forEach { (groupTitle, items) ->
                        item {
                            Text(
                                groupTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfSlate400,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        items(items) { item ->
                            BentoCard(
                                backgroundColor = Color.White,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    onSelectStepType(item.kind)
                                    onClose()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LfSlate50)
                                            .border(1.dp, LfSlate200, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.iconEmoji, fontSize = 20.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                            if (item.tag != null) {
                                                BentoBadge(item.tag, LfEmerald100, LfEmerald800, LfEmerald300)
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            item.desc,
                                            fontSize = 11.sp,
                                            color = LfSlate500,
                                            lineHeight = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text("›", fontSize = 18.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💡 建议优先使用“点击文字”适配不同屏幕",
                        fontSize = 10.sp,
                        color = LfSlate400
                    )
                    BentoOutlinedButton("取消", onClick = onClose)
                }
            }
        }
    }
}

// =========================================================================
// 3. Confirm Dialog (通用确认弹窗)
// =========================================================================
@Composable
fun BentoConfirmDialog(
    isOpen: Boolean,
    title: String,
    description: String,
    confirmText: String = "确认",
    cancelText: String = "取消",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDestructive) LfRose700 else LfSlate900)
        },
        text = {
            Text(description, fontSize = 13.sp, color = LfSlate600, lineHeight = 18.sp)
        },
        confirmButton = {
            BentoButton(
                text = confirmText,
                onClick = onConfirm,
                backgroundColor = if (isDestructive) LfRose500 else LfEmerald500
            )
        },
        dismissButton = {
            BentoOutlinedButton(
                text = cancelText,
                onClick = onDismiss
            )
        }
    )
}
