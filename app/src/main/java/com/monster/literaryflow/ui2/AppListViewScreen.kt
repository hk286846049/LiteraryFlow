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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.TaskModel

@Composable
fun AppListViewScreen(
    installedApps: List<InstalledAppOption>,
    document: AutomationDocument,
    onToggleAppFavorite: (String) -> Unit,
    onToggleAppOrientation: (String, Boolean) -> Unit,
    onCreateTaskForApp: (String, String) -> Unit,
    onSelectTask: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForDetail by remember { mutableStateOf<InstalledAppOption?>(null) }

    val defaultApps = remember {
        listOf(
            InstalledAppOption("明日方舟 (Arknights)", "com.hypergryph.arknights"),
            InstalledAppOption("崩坏：星穹铁道", "com.mihoyo.starrail"),
            InstalledAppOption("广告领宝箱 (AdRewards)", "com.pangle.adreward"),
            InstalledAppOption("短视频自动宝箱", "com.kuaishou.nebula"),
            InstalledAppOption("王者荣耀", "com.tencent.tmgp.sgame")
        )
    }

    val effectiveApps = remember(installedApps) {
        installedApps.ifEmpty { defaultApps }
    }

    val filteredApps = remember(effectiveApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) effectiveApps
        else effectiveApps.filter { app ->
            app.label.lowercase().contains(q) || app.packageName.lowercase().contains(q)
        }
    }

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
                            Text("📱", fontSize = 20.sp)
                            Text("目标应用管理", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                        }
                        Text(
                            "查看常用手游与广告工具，并为不同 App 设置横竖屏自动化偏好",
                            fontSize = 11.sp,
                            color = LfSlate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // 2. Search Box
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                LfInputField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "🔍  搜索应用名称或包名...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Apps Grid / List
        items(filteredApps, key = { it.packageName }) { app ->
            val linkedTasks = document.tasks.filter { it.targetPackageName == app.packageName }
            val iconEmoji = getAppCategoryIcon("game", app.label)

            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp),
                onClick = { selectedAppForDetail = app }
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(LfSlate50)
                                    .border(1.dp, LfSlate200, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(iconEmoji, fontSize = 26.sp)
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        app.label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LfSlate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { onToggleAppFavorite(app.packageName) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("★", fontSize = 14.sp, color = LfSlate300)
                                    }
                                }
                                Text(
                                    app.packageName,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = LfSlate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("横屏显示", fontSize = 10.sp, color = LfSlate500)
                                    Text("•", fontSize = 10.sp, color = LfSlate300)
                                    Text(
                                        "关联 ${linkedTasks.size} 个任务",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LfEmerald700
                                    )
                                }
                            }
                        }

                        Text("›", fontSize = 18.sp, color = LfSlate300)
                    }

                    Divider(color = LfSlate100)

                    // Bottom Action Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("最近使用：近期", fontSize = 11.sp, color = LfSlate400)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LfEmerald50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LfEmerald200),
                            modifier = Modifier.clickable {
                                onCreateTaskForApp(app.label, app.packageName)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfEmerald800)
                                Text(
                                    "新建该 App 任务",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LfEmerald800
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // App Detail Modal
    selectedAppForDetail?.let { targetApp ->
        val appTasks = document.tasks.filter { it.targetPackageName == targetApp.packageName }
        AppDetailModalDialog(
            app = targetApp,
            tasks = appTasks,
            onClose = { selectedAppForDetail = null },
            onOpenCreate = {
                val current = targetApp
                selectedAppForDetail = null
                onCreateTaskForApp(current.label, current.packageName)
            },
            onSelectTask = { taskId ->
                selectedAppForDetail = null
                onSelectTask(taskId)
            }
        )
    }
}

@Composable
fun AppDetailModalDialog(
    app: InstalledAppOption,
    tasks: List<TaskModel>,
    onClose: () -> Unit,
    onOpenCreate: () -> Unit,
    onSelectTask: (Long) -> Unit
) {
    var orientation by remember { mutableStateOf("landscape") }
    var autoLaunch by remember { mutableStateOf(true) }
    val iconEmoji = getAppCategoryIcon("game", app.label)

    WizardModalShell(onClose = onClose) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LfSlate50)
                                .border(1.dp, LfSlate200, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(iconEmoji, fontSize = 24.sp)
                        }
                        Column {
                            Text(app.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                            Text(app.packageName, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = LfSlate400)
                            Text("最近使用：近期活跃", fontSize = 10.sp, color = LfSlate500)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 14.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = LfSlate150)

                // Preferences
                BentoCard(
                    backgroundColor = LfSlate50,
                    borderColor = LfSlate200,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📱", fontSize = 14.sp)
                            Text("应用自动化偏好设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                        }

                        Column {
                            Text("默认屏幕旋转方向", fontSize = 11.sp, color = LfSlate600)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "auto" to "自动适应",
                                    "landscape" to "锁定横屏",
                                    "portrait" to "锁定竖屏"
                                ).forEach { (oKey, oLabel) ->
                                    val isSel = orientation == oKey
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) LfEmerald600 else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) LfEmerald600 else LfSlate200),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { orientation = oKey }
                                    ) {
                                        Text(
                                            oLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else LfSlate700,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("执行时自动切换前台启动", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LfSlate700)
                            Switch(
                                checked = autoLaunch,
                                onCheckedChange = { autoLaunch = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LfEmerald500
                                )
                            )
                        }
                    }
                }

                // Linked Tasks List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("关联该 App 的自动化任务 (${tasks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                        Text(
                            "+ 新建任务",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LfEmerald700,
                            modifier = Modifier.clickable { onOpenCreate() }
                        )
                    }

                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LfSlate50)
                                .border(1.dp, LfSlate200, RoundedCornerShape(14.dp))
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("当前应用尚未创建任何自动化任务", fontSize = 11.sp, color = LfSlate400)
                        }
                    } else {
                        tasks.take(4).forEach { t ->
                            BentoCard(
                                backgroundColor = LfSlate50,
                                borderColor = LfSlate200,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(10.dp),
                                onClick = { onSelectTask(t.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(t.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfSlate900)
                                        Text("${t.steps.size} 个步骤 · 今日已运行 ${t.todayRunCount} 次", fontSize = 10.sp, color = LfSlate500)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LfEmerald50),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("▶", fontSize = 11.sp, color = LfEmerald700)
                                    }
                                }
                            }
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    BentoOutlinedButton(
                        text = "关闭",
                        onClick = onClose,
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }
    }
