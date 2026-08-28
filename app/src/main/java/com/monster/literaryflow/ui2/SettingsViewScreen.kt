package com.monster.literaryflow.ui2

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsViewScreen(
    settings: Map<String, Boolean>,
    numericSettings: Map<String, Int>,
    onSettingChanged: (String, Boolean) -> Unit,
    onNumericSettingChanged: (String, Int) -> Unit,
    onClearHistory: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenMoreTools: () -> Unit
) {
    var confirmClearHistory by remember { mutableStateOf(false) }

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
                            Text("⚙️", fontSize = 20.sp)
                            Text("偏好与系统设置", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                        }
                        Text("运行策略、悬浮控制器、数据备份与 OCR 引擎设置", fontSize = 11.sp, color = LfSlate500, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        // 2. Global Execution Defaults
        item {
            SectionHeader("自动化执行策略", "所有任务默认继承的容错与交互行为")
        }

        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggleRow(
                        title = "默认自动拉起前台 App",
                        subtitle = "创建新任务时默认勾选“拉起前台应用”",
                        checked = settings["autoOpenAppDefault"] ?: true,
                        onCheckedChange = { onSettingChanged("autoOpenAppDefault", it) }
                    )

                    Divider(color = LfSlate100)

                    SettingToggleRow(
                        title = "开启屏幕常亮保护",
                        subtitle = "自动化执行期间保持手机屏幕常亮，防止锁屏休眠",
                        checked = settings["keepScreenOn"] ?: true,
                        onCheckedChange = { onSettingChanged("keepScreenOn", it) }
                    )

                    Divider(color = LfSlate100)

                    SettingToggleRow(
                        title = "执行触感振动反馈",
                        subtitle = "任务启动、完成或发生异常时给予轻微震动提示",
                        checked = settings["vibrationFeedback"] ?: true,
                        onCheckedChange = { onSettingChanged("vibrationFeedback", it) }
                    )

                    Divider(color = LfSlate100)

                    SettingToggleRow(
                        title = "启用悬浮控制球",
                        subtitle = "在其他 App 上方展示迷你控制球，随时暂停/中止任务",
                        checked = settings["enableFloatingController"] ?: true,
                        onCheckedChange = { onSettingChanged("enableFloatingController", it) }
                    )
                }
            }
        }

        // 3. Data Backup & Tools
        item {
            SectionHeader("数据备份与导入导出", "备份你的任务编排方案或迁移到其他设备")
        }

        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("导出任务配置为 JSON", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                            Text("将当前所有任务步骤配置复制到剪贴板", fontSize = 11.sp, color = LfSlate500)
                        }
                        BentoButton("导出配置", onClick = onExportJson, backgroundColor = LfSky500, modifier = Modifier.height(34.dp))
                    }

                    Divider(color = LfSlate100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("导入任务配置 JSON", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                            Text("从剪贴板粘贴或导入外部 Schema 文件", fontSize = 11.sp, color = LfSlate500)
                        }
                        BentoButton("导入任务", onClick = onImportJson, backgroundColor = LfEmerald500, modifier = Modifier.height(34.dp))
                    }

                    Divider(color = LfSlate100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("清空所有运行日志", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LfRose700)
                            Text("重置历史成功率统计与已保存的执行时间线", fontSize = 11.sp, color = LfSlate500)
                        }
                        BentoButton("清空记录", onClick = { confirmClearHistory = true }, backgroundColor = LfRose500, modifier = Modifier.height(34.dp))
                    }
                }
            }
        }

        // 4. System & Extended Tools
        item {
            SectionHeader("调试与扩展功能", "权限检查与高级工具面板")
        }

        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPermissions() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️", fontSize = 16.sp)
                            Text("系统权限与运行环境中心", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                        }
                        Text("›", fontSize = 16.sp, color = LfSlate400)
                    }

                    Divider(color = LfSlate100)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMoreTools() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🧩", fontSize = 16.sp)
                            Text("高级调试工具与 31 个原型页索引", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                        }
                        Text("›", fontSize = 16.sp, color = LfSlate400)
                    }
                }
            }
        }
    }

    if (confirmClearHistory) {
        BentoConfirmDialog(
            isOpen = true,
            title = "清空所有运行日志记录？",
            description = "清空后执行耗时与日志时间轴将无法找回。",
            isDestructive = true,
            confirmText = "确认清空",
            onConfirm = {
                confirmClearHistory = false
                onClearHistory()
            },
            onDismiss = { confirmClearHistory = false }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
            Text(subtitle, fontSize = 11.sp, color = LfSlate500)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LfEmerald500,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = LfSlate300
            )
        )
    }
}
