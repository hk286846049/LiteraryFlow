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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionCenterViewScreen(
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    screenCaptureGranted: Boolean,
    batteryOptimizationGranted: Boolean,
    onBack: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onStartCapture: () -> Unit
) {
    val allCoreGranted = accessibilityGranted && overlayGranted && screenCaptureGranted

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
                    BentoOutlinedButton(text = "‹ 返回", onClick = onBack, modifier = Modifier.height(36.dp))
                    Text("系统权限与运行环境", fontSize = 15.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                    BentoBadge(
                        if (allCoreGranted) "全部就绪" else "待授权",
                        if (allCoreGranted) LfEmerald50 else LfAmber50,
                        if (allCoreGranted) LfEmerald700 else LfAmber800,
                        if (allCoreGranted) LfEmerald200 else LfAmber200
                    )
                }
            }
        }

        // Summary Card
        item {
            BentoCard(
                backgroundColor = if (allCoreGranted) LfEmerald50 else LfAmber100,
                borderColor = if (allCoreGranted) LfEmerald200 else LfAmber200,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (allCoreGranted) LfEmerald200 else LfAmber200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (allCoreGranted) "🛡️" else "⚠️", fontSize = 24.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (allCoreGranted) "核心运行环境已全部就绪！" else "部分核心权限尚未授权",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (allCoreGranted) LfEmerald900 else LfAmber900
                        )
                        Text(
                            if (allCoreGranted) "无障碍点击、屏幕内容捕获与悬浮控制均可正常执行" else "请按以下提示逐项开启系统权限以确保自动化顺利执行",
                            fontSize = 11.sp,
                            color = if (allCoreGranted) LfEmerald800 else LfAmber800,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Core Permissions Section
        item {
            SectionHeader("核心必要权限 (必须全部开启)", "自动化模拟手势与文字识别的基础依赖")
        }

        // 1. Accessibility Service
        item {
            PermissionCardItem(
                name = "无障碍服务 (Accessibility Service)",
                description = "用于模拟屏幕点击、手势滑动、长按以及返回键操作",
                isGranted = accessibilityGranted,
                tip = "若遇到点击失效，可进入系统设置重新开关一次服务",
                onAction = onOpenAccessibilitySettings
            )
        }

        // 2. Screen Capture
        item {
            PermissionCardItem(
                name = "屏幕内容识别 (OCR & Screen Capture)",
                description = "用于实时分析手游与广告页面上的文字、按钮位置",
                isGranted = screenCaptureGranted,
                tip = "支持毫秒级本地离线字元识别，无需上传网络",
                onAction = onStartCapture
            )
        }

        // 3. Floating Overlay
        item {
            PermissionCardItem(
                name = "悬浮窗权限 (Display over other apps)",
                description = "用于在游戏和广告上方显示迷你控制悬浮球与运行进度条",
                isGranted = overlayGranted,
                tip = "运行任务时随时可由悬浮窗一键暂停或停止",
                onAction = onOpenOverlaySettings
            )
        }

        // Extended Permissions Section
        item {
            SectionHeader("扩展运行优化权限 (推荐开启)", "提升后台长效运行与防被系统休眠清理的能力")
        }

        // 4. Battery Optimization
        item {
            PermissionCardItem(
                name = "忽略电池优化 (Battery Optimization)",
                description = "防止 Android 系统在手机锁屏或息屏时误杀后台自动化进程",
                isGranted = batteryOptimizationGranted,
                tip = "允许后台长时间稳定执行循环与定时调度",
                onAction = onOpenBatterySettings
            )
        }
    }
}

@Composable
private fun PermissionCardItem(
    name: String,
    description: String,
    isGranted: Boolean,
    tip: String,
    onAction: () -> Unit
) {
    BentoCard(
        backgroundColor = Color.White,
        borderColor = LfSlate200,
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                    Text(description, fontSize = 11.sp, color = LfSlate500, modifier = Modifier.padding(top = 2.dp))
                }

                BentoBadge(
                    if (isGranted) "已授权" else "去开启",
                    if (isGranted) LfEmerald50 else LfRose50,
                    if (isGranted) LfEmerald700 else LfRose700,
                    if (isGranted) LfEmerald200 else LfRose200,
                    modifier = Modifier.clickable { onAction() }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("💡 $tip", fontSize = 10.sp, color = LfSlate400)
        }
    }
}
