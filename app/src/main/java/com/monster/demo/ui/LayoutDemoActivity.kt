package com.monster.demo.ui
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

class LayoutDemoActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // 使用 Scaffold 作为基础布局
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Compose 布局大全") })
                    }
                ) { paddingValues ->
                    // 主内容区域使用可滚动布局
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item { BoxLayoutDemo() }
                        item { ColumnRowDemo() }
                        item { BoxWithConstraintsDemo() }
                        item { ConstraintLayoutDemo() }
                        item { CustomLayoutDemo() }
                        item { SpacerAndPaddingDemo() }
                        item { WeightDemo() }
                        item { LazyLayoutDemo() }
                        item { StaggeredGridDemo() }
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
            val itemConstraints = constraints.copy(maxWidth = columnWidth)

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