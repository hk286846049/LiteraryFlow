package com.monster.demo.ui
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.constraintlayout.compose.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LayoutDemoActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("布局", "动画", "手势", "主题", "高级")

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Compose 功能大全") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { index, title ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = when (index) {
                                                0 -> Icons.Default.Home
                                                1 -> Icons.Default.PlayArrow
                                                2 -> Icons.Default.DateRange
                                                3 -> Icons.Default.Person
                                                else -> Icons.Default.Star
                                            },
                                            contentDescription = title
                                        )
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                item { BoxLayoutDemo() }
                                item { ColumnRowDemo() }
                                item { BoxWithConstraintsDemo() }
                                item { ConstraintLayoutDemo() }
                                item { CustomLayoutDemo() }
                                item { SpacerAndPaddingDemo() }
                                item { WeightDemo() }
                                item { LazyLayoutDemo() }
                                item { StaggeredGridDemo() }
                                item { GridLayoutDemo() }
                            }
                            1 -> {
                                item { AnimationDemo() }
                                item { TransitionDemo() }
                                item { GestureAnimationDemo() }
                            }
                            2 -> {
                                item { GestureDemo() }
                                item { DragDemo() }
                                item { SwipeDemo() }
                            }
                            3 -> {
                                item { ThemeDemo() }
                                item { ColorDemo() }
                                item { TypographyDemo() }
                            }
                            else -> {
                                item { AdvancedLayoutDemo() }
                                item { CustomModifierDemo() }
                                item { StateManagementDemo() }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------- 布局示例 --------------------

/**
 * Box 布局示例 - 叠加布局
 */
@Preview
@Composable
fun BoxLayoutDemo() {
    // Box 允许子项叠加在一起
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 背景层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        )

        // 居中文本
        Text(
            text = "居中文本",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 24.sp
        )

        // 右上角图标
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.Yellow,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )

        // 底部对齐的按钮
        Button(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text("底部按钮")
        }
    }
}

/**
 * Column 和 Row 基础布局示例
 */
@Preview
@Composable
fun ColumnRowDemo() {
    // 垂直排列的 Column
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Column 示例", style = MaterialTheme.typography.headlineSmall)

        // 嵌套的 Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFBB86FC))
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${index + 1}", color = Color.White)
                }
            }
        }

        // 另一个嵌套的 Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("这是一个信息提示", fontWeight = FontWeight.Bold)
        }
    }
}


/**
 * BoxWithConstraints 示例 - 响应式布局
 */
@Preview
@Composable
fun BoxWithConstraintsDemo() {
    // BoxWithConstraints 可以获取父容器的约束条件
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color(0xFFE1BEE7))
    ) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text("BoxWithConstraints 示例", style = MaterialTheme.typography.headlineSmall)
            Text("宽度: ${boxWidth.value}dp")
            Text("高度: ${boxHeight.value}dp")

            // 根据容器宽度调整的文本
            Text(
                text = if (boxWidth < 300.dp) "小屏幕" else "大屏幕",
                color = Color(0xFF6200EE),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * ConstraintLayout 示例 - 复杂相对布局
 */
@Preview
@Composable
fun ConstraintLayoutDemo() {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFDCEDC8))
    ) {
        // 创建引用
        val (imageRef, titleRef, descRef, buttonRef) = createRefs()

        // 图片
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF8BC34A), CircleShape)
                .constrainAs(imageRef) {
                    top.linkTo(parent.top, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                }
        )

        // 标题
        Text(
            text = "ConstraintLayout 示例",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.constrainAs(titleRef) {
                top.linkTo(imageRef.top)
                start.linkTo(imageRef.end, 16.dp)
                end.linkTo(parent.end, 16.dp)
                width = Dimension.fillToConstraints
            }
        )

        // 描述
        Text(
            text = "这是一个使用 ConstraintLayout 构建的复杂布局示例",
            modifier = Modifier.constrainAs(descRef) {
                top.linkTo(titleRef.bottom, 8.dp)
                start.linkTo(titleRef.start)
                end.linkTo(titleRef.end)
                width = Dimension.fillToConstraints
            }
        )

        // 按钮
        Button(
            onClick = {},
            modifier = Modifier.constrainAs(buttonRef) {
                bottom.linkTo(parent.bottom, 16.dp)
                end.linkTo(parent.end, 16.dp)
            }
        ) {
            Text("确认")
        }
    }
}

/**
 * 自定义布局示例
 */
@Preview
@Composable
fun CustomLayoutDemo() {
    // 自定义布局函数
    @Composable
    fun CustomLayout(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Layout(
            modifier = modifier,
            content = content
        ) { measurables, constraints ->
            // 测量所有子项
            val placeables = measurables.map { it.measure(constraints) }

            // 计算总宽度和最大高度
            val width = placeables.sumOf { it.width }
            val height = placeables.maxOfOrNull { it.height } ?: 0

            // 布局
            layout(width, height) {
                var x = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(x = x, y = 0)
                    x += placeable.width
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("自定义布局示例", style = MaterialTheme.typography.headlineSmall)

        CustomLayout(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFFB3E5FC))
        ) {
            Box(modifier = Modifier.size(60.dp).background(Color.Red))
            Box(modifier = Modifier.size(70.dp).background(Color.Blue))
            Box(modifier = Modifier.size(50.dp).background(Color.Green))
            Box(modifier = Modifier.size(80.dp).background(Color.Yellow))
        }
    }
}

/**
 * Spacer 和 Padding 示例
 */
@Composable
fun SpacerAndPaddingDemo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF9C4))
            .padding(16.dp) // 外部 padding
    ) {
        Text("Spacer 和 Padding 示例", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp) // 内部 padding
        ) {
            Text("Item 1")
            Spacer(Modifier.weight(1f)) // 弹性空间
            Text("Item 2")
        }

        // 垂直间距
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("左边", modifier = Modifier.padding(end = 8.dp))
            Text("右边", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * Weight 权重示例
 */
@Composable
fun WeightDemo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("Weight 权重示例", style = MaterialTheme.typography.headlineSmall)

        // 水平权重分配
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Red)
            ) {
                Text("1", modifier = Modifier.align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(Color.Blue)
            ) {
                Text("2", modifier = Modifier.align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Green)
            ) {
                Text("1", modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.height(16.dp))

        // 垂直权重分配
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .background(Color(0xFFFFCC80))
            ) {
                Text("3", modifier = Modifier.align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFFFE0B2))
            ) {
                Text("1", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * 懒加载布局示例
 */
@Preview
@Composable
fun LazyLayoutDemo() {
    // 模拟数据
    val items = (1..20).map { "项目 $it" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("Lazy 懒加载布局示例", style = MaterialTheme.typography.headlineSmall)

        // 水平懒加载列表
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(items.take(5)) { item ->
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFC8E6C9))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 垂直懒加载列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFFBBDEFB))
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(item, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 瀑布流布局示例
 */

@Composable
fun StaggeredGridDemo() {
    // 模拟不同高度的数据
    val items = listOf(
        "短文本" to 80.dp,
        "中等长度的文本内容" to 120.dp,
        "这是一个比较长的文本项目，用来展示不同高度的布局" to 160.dp,
        "短" to 60.dp,
        "中等项目" to 100.dp,
        "这是一个非常长的文本内容，用来测试瀑布流布局的自适应高度效果" to 180.dp,
        "普通项目" to 90.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("瀑布流布局示例", style = MaterialTheme.typography.headlineSmall)

        // 自定义瀑布流布局
        Layout(
            content = {
                items.forEach { (text, height) ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color(0xFFF8BBD0))
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            // 两列布局
            val columnWidth = constraints.maxWidth / 2
            val itemConstraints = constraints.copy(
                minWidth = 0,  // 添加 minWidth 约束
                maxWidth = columnWidth
            )
            // 测量所有子项
            val placeables = measurables.map { it.measure(itemConstraints) }

            // 跟踪每列的当前高度
            val columnHeights = IntArray(2) { 0 }

            // 确定每个项目的位置
            val itemPositions = placeables.map { placeable ->
                val column = columnHeights.withIndex().minByOrNull { it.value }?.index ?: 0
                val position = IntOffset(
                    x = column * columnWidth,
                    y = columnHeights[column]
                )
                columnHeights[column] += placeable.height
                position
            }

            // 计算总高度
            val totalHeight = columnHeights.maxOrNull() ?: 0

            layout(constraints.maxWidth, totalHeight) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(
                        x = itemPositions[index].x,
                        y = itemPositions[index].y
                    )
                }
            }
        }
    }
}

/**
 * 网格布局示例
 */
@Preview
@Composable
fun GridLayoutDemo() {
    val items = (1..20).map { "Item $it" }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("Grid 布局示例", style = MaterialTheme.typography.headlineSmall)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item)
                    }
                }
            }
        }
    }
}

/**
 * 动画示例
 */
@Preview
@Composable
fun AnimationDemo() {
    var isExpanded by remember { mutableStateOf(false) }
    val transition = updateTransition(isExpanded, label = "expand")
    
    val size by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "size"
    ) { expanded ->
        if (expanded) 200.dp else 100.dp
    }
    
    val rotation by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 500)
        },
        label = "rotation"
    ) { expanded ->
        if (expanded) 360f else 0f
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("动画示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isExpanded) "点击收缩" else "点击展开",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * 转场动画示例
 */
@Preview
@Composable
fun TransitionDemo() {
    var visible by remember { mutableStateOf(true) }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("转场动画示例", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("这是一个带有转场动画的卡片")
            }
        }
    }
    
    Button(
        onClick = { visible = !visible },
        modifier = Modifier.padding(8.dp)
    ) {
        Text(if (visible) "隐藏" else "显示")
    }
}

/**
 * 手势动画示例
 */
@Preview
@Composable
fun GestureAnimationDemo() {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .size(100.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            scope.launch {
                                offset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("点击重置")
            }
        }
    }
}

/**
 * 手势示例
 */
@Preview
@Composable
fun GestureDemo() {
    var text by remember { mutableStateOf("点击或长按") }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { text = "点击" },
                    onLongPress = { text = "长按" },
                    onDoubleTap = { text = "双击" }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * 拖拽示例
 */
@Preview
@Composable
fun DragDemo() {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(100.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("拖拽我")
            }
        }
    }
}

/**
 * 滑动示例
 */
@Preview
@Composable
fun SwipeDemo() {
    var offsetX by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .height(80.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                offsetX = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        offsetX += dragAmount
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("左右滑动")
            }
        }
    }
}

/**
 * 主题示例
 */
@Preview
@Composable
fun ThemeDemo() {
    var isDarkTheme by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("主题示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "当前主题: ${if (isDarkTheme) "深色" else "浅色"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { isDarkTheme = !isDarkTheme },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("切换主题")
                }
            }
        }
    }
}

/**
 * 颜色示例
 */
@Preview
@Composable
fun ColorDemo() {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("颜色示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(color, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

/**
 * 排版示例
 */
@Preview
@Composable
fun TypographyDemo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("排版示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Display Large",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            "Display Medium",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            "Display Small",
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            "Headline Large",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            "Headline Medium",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Headline Small",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Title Large",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Title Medium",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Title Small",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Body Large",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Body Medium",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Body Small",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Label Large",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            "Label Medium",
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            "Label Small",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * 高级布局示例
 */
@Preview
@Composable
fun AdvancedLayoutDemo() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("标签1", "标签2", "标签3")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("高级布局示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("标签1内容")
                }
            }
            1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("标签2内容")
                }
            }
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("标签3内容")
                }
            }
        }
    }
}

/**
 * 自定义修饰符示例
 */
@Preview
@Composable
fun CustomModifierDemo() {
    fun Modifier.customBackground(
        color: Color = Color.Gray,
        alpha: Float = 0.2f
    ) = composed {
        this
            .background(color.copy(alpha = alpha))
            .padding(8.dp)
            .border(1.dp, color, RoundedCornerShape(8.dp))
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("自定义修饰符示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .customBackground(
                    color = MaterialTheme.colorScheme.primary,
                    alpha = 0.1f
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("自定义背景修饰符")
        }
    }
}

/**
 * 状态管理示例
 */
@Preview
@Composable
fun StateManagementDemo() {
    var count by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text("状态管理示例", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "计数: $count",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { count-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("减少")
                    }
                    
                    Button(
                        onClick = { count++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("增加")
                    }
                }
            }
        }
    }
}