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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.core.model.AutomationDocument
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.TaskModel

@Composable
fun TaskListViewScreen(
    document: AutomationDocument,
    selectedTaskId: Long?,
    onCreateTask: () -> Unit,
    onOpenTemplates: () -> Unit,
    onDeleteTask: (TaskModel) -> Unit,
    onRequestDeleteTask: (TaskModel) -> Unit,
    onRunTask: (TaskModel) -> Unit,
    onToggleTaskEnabled: (TaskModel, Boolean) -> Unit,
    onToggleTaskFavorite: (TaskModel) -> Unit,
    onDuplicateTask: (TaskModel) -> Unit,
    onEditTask: (TaskModel) -> Unit,
    onBatchExport: (List<TaskModel>) -> Unit,
    onSelectTask: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("all") } // all, game, ad, general, favorite, enabled, disabled
    var sortBy by remember { mutableStateOf("recent") } // recent, name, duration
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmBatchDelete by remember { mutableStateOf(false) }

    val tasks = document.tasks

    val filteredTasks = tasks
        .filter { task ->
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    task.title.lowercase().contains(q) ||
                    task.targetAppName.lowercase().contains(q) ||
                    task.targetPackageName.lowercase().contains(q) ||
                    task.steps.any { it.title.lowercase().contains(q) || it.actions.any { a -> a.text?.lowercase()?.contains(q) == true } }

            val matchesCategory = when (activeCategory) {
                "game" -> task.category == "game" || task.targetAppName.contains("舟") || task.targetAppName.contains("铁") || task.targetAppName.contains("原神")
                "ad" -> task.category == "ad" || task.targetAppName.contains("广告") || task.targetAppName.contains("宝箱") || task.title.contains("广告")
                "general" -> task.category == "general" || (task.category.isBlank() && !task.targetAppName.contains("舟") && !task.title.contains("广告"))
                "favorite" -> task.favorite
                "enabled" -> task.enabled
                "disabled" -> !task.enabled
                else -> true
            }

            matchesQuery && matchesCategory
        }
        .sortedWith { a, b ->
            if (a.favorite != b.favorite) {
                if (a.favorite) -1 else 1
            } else {
                when (sortBy) {
                    "name" -> a.title.compareTo(b.title, ignoreCase = true)
                    "duration" -> a.estimatedDurationMs.compareTo(b.estimatedDurationMs)
                    else -> b.updatedAt.compareTo(a.updatedAt)
                }
            }
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Banner
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
                            Text("📑", fontSize = 20.sp)
                            Text("自动化任务库", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LfEmerald800)
                        }
                        Text(
                            "编排、配置与随时触发你的手游日常与广告自动化任务",
                            fontSize = 11.sp,
                            color = LfSlate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoOutlinedButton(
                            text = "模板库",
                            onClick = onOpenTemplates,
                            borderColor = LfEmerald200,
                            contentColor = LfEmerald700,
                            modifier = Modifier.height(36.dp)
                        )
                        BentoButton(
                            text = "+ 新建",
                            onClick = onCreateTask,
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        }

        // 2. Search & Category Filters Bar
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索任务名称、App 或动作步骤...", fontSize = 12.sp, color = LfSlate400) },
                        singleLine = true,
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    // Categories pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "all" to "全部任务",
                            "game" to "手游日常",
                            "ad" to "广告处理",
                            "general" to "通用任务",
                            "favorite" to "★ 收藏",
                            "enabled" to "已启用",
                            "disabled" to "已停用"
                        ).forEach { (catKey, catLabel) ->
                            val isSelected = activeCategory == catKey
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (isSelected) LfEmerald500 else LfSlate50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) LfEmerald500 else LfSlate200),
                                modifier = Modifier.clickable { activeCategory = catKey }
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else LfSlate600,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Sort & Batch toggle bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("排序:", fontSize = 11.sp, color = LfSlate400, fontWeight = FontWeight.Bold)
                            listOf("recent" to "最近更新", "name" to "名称", "duration" to "预估耗时").forEach { (sortKey, sortLabel) ->
                                val isSelected = sortBy == sortKey
                                TextButton(
                                    onClick = { sortBy = sortKey },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        sortLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) LfEmerald600 else LfSlate500
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                isBatchMode = !isBatchMode
                                selectedTaskIds = emptySet()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isBatchMode) "退出批量" else "批量管理",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBatchMode) LfEmerald600 else LfSlate500
                            )
                        }
                    }
                }
            }
        }

        // 3. Batch Action Bar (if active and tasks selected)
        if (isBatchMode) {
            item {
                BentoCard(
                    backgroundColor = LfEmerald50,
                    borderColor = LfEmerald200,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("已选择 ${selectedTaskIds.size}/${filteredTasks.size} 个任务", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LfEmerald900)
                            TextButton(
                                onClick = {
                                    selectedTaskIds = if (selectedTaskIds.size == filteredTasks.size) emptySet() else filteredTasks.map { it.id }.toSet()
                                }
                            ) {
                                Text(if (selectedTaskIds.size == filteredTasks.size) "取消全选" else "全选", fontSize = 11.sp, color = LfEmerald700, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BentoButton(
                                text = "批量启用",
                                onClick = {
                                    tasks.filter { it.id in selectedTaskIds }.forEach { onToggleTaskEnabled(it, true) }
                                    selectedTaskIds = emptySet()
                                },
                                backgroundColor = LfEmerald600,
                                modifier = Modifier.weight(1f).height(34.dp)
                            )
                            BentoButton(
                                text = "批量停用",
                                onClick = {
                                    tasks.filter { it.id in selectedTaskIds }.forEach { onToggleTaskEnabled(it, false) }
                                    selectedTaskIds = emptySet()
                                },
                                backgroundColor = LfSlate600,
                                modifier = Modifier.weight(1f).height(34.dp)
                            )
                            BentoButton(
                                text = "导出 JSON",
                                onClick = {
                                    onBatchExport(tasks.filter { it.id in selectedTaskIds })
                                    selectedTaskIds = emptySet()
                                },
                                backgroundColor = LfSky500,
                                modifier = Modifier.weight(1f).height(34.dp)
                            )
                            BentoButton(
                                text = "删除",
                                onClick = { confirmBatchDelete = true },
                                backgroundColor = LfRose500,
                                modifier = Modifier.weight(1f).height(34.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Task Cards List
        if (filteredTasks.isEmpty()) {
            item {
                BentoEmptyState(
                    title = "暂无匹配的自动化任务",
                    description = "可尝试调整筛选分类、关键字，或点击下方立即新建",
                    actionText = "新建自动化任务",
                    onAction = onCreateTask,
                    iconEmoji = "📋"
                )
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                val isSelected = selectedTaskIds.contains(task.id)
                val iconEmoji = getAppCategoryIcon(task.category, task.targetAppName)

                BentoCard(
                    backgroundColor = if (isSelected) LfEmerald50.copy(alpha = 0.5f) else Color.White,
                    borderColor = if (isSelected) LfEmerald500 else if (!task.enabled) LfSlate200.copy(alpha = 0.6f) else LfSlate200,
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(16.dp),
                    onClick = {
                        if (isBatchMode) {
                            selectedTaskIds = if (isSelected) selectedTaskIds - task.id else selectedTaskIds + task.id
                        } else {
                            onSelectTask(task.id)
                        }
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isBatchMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedTaskIds = if (isSelected) selectedTaskIds - task.id else selectedTaskIds + task.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = LfEmerald500)
                                )
                            }

                            // App Icon Box
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LfSlate50)
                                    .border(1.dp, LfSlate200, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(iconEmoji, fontSize = 22.sp)
                            }

                            // Title & Description
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        task.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.enabled) LfSlate900 else LfSlate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (task.favorite) "★" else "☆",
                                        fontSize = 16.sp,
                                        color = if (task.favorite) Color(0xFFF59E0B) else LfSlate300,
                                        modifier = Modifier.clickable { onToggleTaskFavorite(task) }
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(task.targetAppName.ifBlank { "通用应用" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfSlate700)
                                    Text("•", fontSize = 10.sp, color = LfSlate400)
                                    Text("${task.steps.size} 步骤", fontSize = 11.sp, color = LfSlate500)
                                    Text("•", fontSize = 10.sp, color = LfSlate400)
                                    Text("约 ${(task.estimatedDurationMs / 60000L).coerceAtLeast(1L)} 分钟", fontSize = 11.sp, color = LfSlate500)
                                }
                            }

                            // Enable Switch
                            Switch(
                                checked = task.enabled,
                                onCheckedChange = { onToggleTaskEnabled(task, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LfEmerald500,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = LfSlate300
                                )
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Divider(color = LfSlate100)
                        Spacer(Modifier.height(8.dp))

                        // Footer schedule & actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                getScheduleDisplay(task),
                                fontSize = 11.sp,
                                color = LfSlate500
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BentoOutlinedButton(
                                    text = "编辑",
                                    onClick = { onEditTask(task) },
                                    modifier = Modifier.height(30.dp)
                                )
                                BentoButton(
                                    text = "▶ 运行",
                                    onClick = { onRunTask(task) },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Batch Delete Confirmation Dialog
    if (confirmBatchDelete) {
        BentoConfirmDialog(
            isOpen = true,
            title = "确认批量删除 ${selectedTaskIds.size} 个任务？",
            description = "删除后这些任务的步骤编排将无法恢复，是否确认删除？",
            isDestructive = true,
            confirmText = "确认删除",
            onConfirm = {
                tasks.filter { it.id in selectedTaskIds }.forEach(onDeleteTask)
                selectedTaskIds = emptySet()
                confirmBatchDelete = false
            },
            onDismiss = { confirmBatchDelete = false }
        )
    }
}
