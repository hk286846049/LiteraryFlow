package com.monster.demo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class ComposeDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Material 3 主题
            MaterialTheme {
                // 使用 Scaffold 作为页面骨架
                Scaffold(
                    topBar = { TopAppBarExample() },

                ) { paddingValues ->
                    // 使用 LazyColumn 实现可滚动列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { TextExample() }
                        item { ButtonExample() }
                        item { TextFieldExample() }
                        item { SwitchExample() }
                        item { CheckboxExample() }
                        item { RadioButtonExample() }
                        item { SliderExample() }
                        item { ProgressIndicatorExample() }
                        item { IconExample() }
                        item { AlertDialogExample() }
                    }
                }
            }
        }
    }
}

// -------------------- 组件示例 --------------------

// 顶部应用栏示例
@Preview(device = "spec:width=1080px,height=2340px,dpi=440,orientation=landscape")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarExample() {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Compose 组件示例") },
        navigationIcon = {
            IconButton(onClick = { /* 返回按钮点击 */ }) {
                Icon(Icons.Filled.ArrowBack, "返回")
            }
        },
        actions = {
            IconButton(onClick = { /* 搜索按钮点击 */ }) {
                Icon(Icons.Filled.Search, "搜索")
            }

            // 下拉菜单
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, "更多")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("设置") },
                        onClick = { /* 处理设置点击 */ }
                    )
                    DropdownMenuItem(
                        text = { Text("关于") },
                        onClick = { /* 处理关于点击 */ }
                    )
                }
            }
        }
    )
}



// 文本组件示例
@Preview
@Composable
fun TextExample() {
    Column {
        // 基础文本
        Text("基础文本样式")

        // 带样式的文本
        Text(
            text = "带样式的文本",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // 多行文本
        Text(
            text = "这是一个多行文本示例，当文本内容超过一行时会自动换行显示。" +
                    "Compose 的文本组件可以很好地处理多行文本的显示。",
            maxLines = 2
        )
    }
}

// 按钮组件示例
@Preview
@Composable
fun ButtonExample() {
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 加载状态按钮
        Button(
            onClick = { isLoading = true },
            enabled = !isLoading
        ) {

            if (isLoading) {
                // 使用LaunchedEffect启动协程
                LaunchedEffect(key1 = Unit) {
                    delay(2000)
                    isLoading = false
                }
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isLoading) "处理中..." else "带加载状态的按钮")
        }

        // 渐变背景按钮
        Button(
            onClick = { /* 点击事件 */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Text("渐变背景按钮", color = Color.White)
        }
    }
}

// 文本输入框示例
@Preview
@Composable
fun TextFieldExample() {
    var text by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column {
        // 带验证的输入框
        TextField(
            value = text,
            onValueChange = {
                text = it
                isError = it.length > 10
            },
            label = { Text("用户名 (最多10字符)") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(
                        text = "超过最大长度限制",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                if (isError) Icon(Icons.Filled.Warning, "错误")
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 密码强度指示器
        var passwordStrength by remember {
            mutableStateOf(PasswordStrength.WEAK)
        }

        TextField(
            value = password,
            onValueChange = {
                password = it
                passwordStrength = when {
                    it.length > 12 -> PasswordStrength.STRONG
                    it.length > 6 -> PasswordStrength.MEDIUM
                    else -> PasswordStrength.WEAK
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("密码") },
            trailingIcon = {
                Box(modifier = Modifier.width(24.dp)) {
                    when (passwordStrength) {
                        PasswordStrength.WEAK ->
                            Icon(Icons.Filled.Warning, "弱", tint = Color.Red)
                        PasswordStrength.MEDIUM ->
                            Icon(Icons.Filled.Info, "中", tint = Color.Yellow)
                        PasswordStrength.STRONG ->
                            Icon(Icons.Filled.Check, "强", tint = Color.Green)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}


// 开关组件示例
@Preview
@Composable
fun SwitchExample() {
    var checked by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("开关状态: ${if (checked) "开" else "关"}")
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { checked = it }
        )
    }
}

// 复选框组件示例
@Preview
@Composable
fun CheckboxExample() {
    var checked by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it }
        )
        Spacer(Modifier.width(8.dp))
        Text("复选框示例")
    }
}

// 单选按钮组件示例
@Preview
@Composable
fun RadioButtonExample() {
    val options = listOf("选项一", "选项二", "选项三")
    var selectedOption by remember { mutableStateOf(options[0]) }

    Column {
        Text("单选按钮组:", style = MaterialTheme.typography.labelLarge)

        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { selectedOption = option }
                )
                Spacer(Modifier.width(8.dp))
                Text(option)
            }
        }
    }
}

// 滑块组件示例
@Preview
@Composable
fun SliderExample() {
    var sliderValue by remember { mutableStateOf(0f) }

    Column {
        // 自定义滑块样式
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 0f..100f,
            steps = 5,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // 带标记的滑块
        var markedValue by remember { mutableStateOf(30f) }
        val marks = mapOf(
            0f to "低",
            30f to "中",
            70f to "高",
            100f to "最高"
        )

        Slider(
            value = markedValue,
            onValueChange = { markedValue = it },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "当前级别: ${marks[markedValue] ?: ""}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}


// 进度指示器示例
@Preview
@Composable
fun ProgressIndicatorExample() {
    var progress by remember { mutableStateOf(0.3f) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 线性进度条
        Text("线性进度条:")
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )

        // 不确定进度的线性进度条
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        // 圆形进度条
        Text("圆形进度条:")
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(50.dp)
        )

        // 不确定进度的圆形进度条
        CircularProgressIndicator(modifier = Modifier.size(50.dp))
    }
}

// 图标组件示例
@Preview
@Composable
fun IconExample() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 基础图标
        Icon(Icons.Filled.Home, contentDescription = "首页")

        // 带颜色的图标
        Icon(
            Icons.Filled.Favorite,
            contentDescription = "收藏",
            tint = MaterialTheme.colorScheme.error
        )

        // 可点击的图标
        IconButton(onClick = { /* 点击事件 */ }) {
            Icon(Icons.Filled.Star, contentDescription = "评分")
        }
    }
}

// 对话框示例
@Preview
@Composable
fun AlertDialogExample() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }

    Button(onClick = { showDialog = true }) {
        Text("显示复杂对话框")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("请选择") },
            text = {
                Column {
                    Text("这是一个包含多个组件的复杂对话框")
                    Spacer(Modifier.height(16.dp))

                    // 对话框内单选按钮
                    val options = listOf("选项A", "选项B", "选项C")
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = option }
                        ) {
                            RadioButton(
                                selected = option == selectedOption,
                                onClick = { selectedOption = option }
                            )
                            Text(option, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    // 对话框内开关
                    var checked by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("记住选择")
                        Spacer(Modifier.weight(1f))
                        Switch(checked = checked, onCheckedChange = { checked = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}