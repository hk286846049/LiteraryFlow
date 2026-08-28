package com.monster.literaryflow.ui2

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monster.literaryflow.automation.state.RunState
import com.monster.literaryflow.automation.state.RunStatus
import com.monster.literaryflow.core.model.TaskModel
import kotlinx.coroutines.delay

@Composable
fun LiveRunnerViewScreen(
    task: TaskModel,
    runState: RunState,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onCancelRun: () -> Unit,
    onSimulateSuccess: () -> Unit,
    onSimulateFail: (String) -> Unit
) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    var confirmStop by remember { mutableStateOf(false) }
    var showOcrDebug by remember { mutableStateOf(true) }
    var actionState by remember { mutableStateOf("seeking") } // seeking, found, clicking, waiting

    val isPaused = runState.status == RunStatus.PAUSED
    val totalSteps = task.steps.size.coerceAtLeast(1)
    val currentStepIndex = runState.currentStepIndex.coerceIn(1, totalSteps)
    val currentStep = task.steps.getOrNull(currentStepIndex - 1) ?: task.steps.firstOrNull()

    // Timer effect
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (true) {
                delay(1000)
                elapsedSec++
            }
        }
    }

    // Visual animation cycle for simulation
    LaunchedEffect(currentStepIndex, isPaused) {
        if (!isPaused) {
            actionState = "seeking"
            delay(1200)
            actionState = "found"
            delay(1000)
            actionState = "clicking"
            delay(800)
            actionState = "waiting"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Real-time Execution Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulseDot(if (isPaused) LfAmber500 else LfEmerald500, 10.dp)
                            Text(
                                if (isPaused) "执行已暂停" else "自动化任务实时执行中",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) LfAmber800 else LfEmerald700
                            )
                        }

                        // Elapsed timer pill
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = LfSlate100,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "⏱️ ${String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LfSlate700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = LfSlate900)
                    Text("目标应用: ${task.targetAppName} · 前台调度中", fontSize = 12.sp, color = LfSlate500)

                    Spacer(Modifier.height(14.dp))

                    // Progress Bar
                    val progressFloat = (currentStepIndex.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progressFloat,
                        color = LfEmerald500,
                        trackColor = LfSlate100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100.dp))
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("当前进度: 第 $currentStepIndex 步 / 共 $totalSteps 步", fontSize = 11.sp, color = LfSlate500)
                        Text("${(progressFloat * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LfEmerald700)
                    }
                }
            }
        }

        // 2. Current Step Execution Highlight Card
        item {
            BentoCard(
                backgroundColor = LfEmerald50,
                borderColor = LfEmerald200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "当前步骤: ${currentStep?.title ?: "执行中"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LfEmerald900
                        )
                        BentoBadge(
                            when (actionState) {
                                "seeking" -> "🔍 正在扫描文字..."
                                "found" -> "✓ 文字已定位 (98%)"
                                "clicking" -> "👆 正在模拟点击"
                                else -> "⏳ 等待响应延时"
                            },
                            Color.White,
                            LfEmerald800,
                            LfEmerald300
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        currentStep?.let { getStepTypeDescription(it) } ?: "正在识别前台屏幕并匹配特征",
                        fontSize = 12.sp,
                        color = LfEmerald800,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 3. OCR Visual Perception Sandbox Window
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("👁️", fontSize = 16.sp)
                            Text("OCR 视觉感知与捕获调试窗", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        TextButton(onClick = { showOcrDebug = !showOcrDebug }) {
                            Text(if (showOcrDebug) "隐藏监控" else "显示监控", fontSize = 11.sp, color = LfSky400)
                        }
                    }

                    if (showOcrDebug) {
                        Spacer(Modifier.height(10.dp))

                        // Simulated Screen Canvas with OCR bounding box and scan beam
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF090D16))
                                .border(1.dp, LfSlate700, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw target bounding box
                                drawRect(
                                    color = if (actionState == "found" || actionState == "clicking") LfEmerald400 else LfSky400,
                                    topLeft = Offset(size.width * 0.35f, size.height * 0.4f),
                                    size = Size(size.width * 0.3f, size.height * 0.2f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Draw center cross
                                drawCircle(
                                    color = if (actionState == "clicking") LfRose500 else LfEmerald400,
                                    radius = if (actionState == "clicking") 12.dp.toPx() else 6.dp.toPx(),
                                    center = Offset(size.width * 0.5f, size.height * 0.5f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (actionState) {
                                        "seeking" -> "全屏 OCR 字符识别中..."
                                        "found" -> "已匹配: “${currentStep?.actions?.firstOrNull()?.text ?: "目标文字"}”"
                                        "clicking" -> "模拟点击: (X: 0.50, Y: 0.50)"
                                        else -> "步骤已完成，准备流转下一步"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("置信度: 98.4% · 毫秒级本地字元引擎", fontSize = 10.sp, color = LfSlate400)
                            }
                        }
                    }
                }
            }
        }

        // 4. Interactive Live Control Buttons
        item {
            BentoCard(
                backgroundColor = Color.White,
                borderColor = LfSlate200,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("实时交互控制", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LfSlate900)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoButton(
                            text = if (isPaused) "▶ 恢复执行" else "⏸ 暂停任务",
                            onClick = { if (isPaused) onResumeRun() else onPauseRun() },
                            backgroundColor = if (isPaused) LfEmerald600 else LfSlate700,
                            modifier = Modifier.weight(1f).height(42.dp)
                        )
                        BentoButton(
                            text = "⏹ 停止任务",
                            onClick = { confirmStop = true },
                            backgroundColor = LfRose500,
                            modifier = Modifier.weight(1f).height(42.dp)
                        )
                    }

                    Divider(color = LfSlate100)

                    // Prototype Simulator Buttons (for verifying success and failure flows)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoOutlinedButton(
                            text = "✓ 模拟顺利完成",
                            onClick = onSimulateSuccess,
                            borderColor = LfEmerald300,
                            contentColor = LfEmerald700,
                            modifier = Modifier.weight(1f).height(36.dp)
                        )
                        BentoOutlinedButton(
                            text = "⚠️ 模拟识别超时失败",
                            onClick = { onSimulateFail("在第 $currentStepIndex 步寻找目标文字超时未响应 (尝试 3/3 次)") },
                            borderColor = LfRose200,
                            contentColor = LfRose700,
                            modifier = Modifier.weight(1f).height(36.dp)
                        )
                    }
                }
            }
        }
    }

    if (confirmStop) {
        BentoConfirmDialog(
            isOpen = true,
            title = "确认终止当前自动化任务？",
            description = "任务终止后本次执行记录将标记为已取消，不会影响历史数据。",
            isDestructive = true,
            confirmText = "终止任务",
            onConfirm = {
                confirmStop = false
                onCancelRun()
            },
            onDismiss = { confirmStop = false }
        )
    }
}
