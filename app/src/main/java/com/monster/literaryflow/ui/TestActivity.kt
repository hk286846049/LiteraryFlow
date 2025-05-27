package com.monster.literaryflow.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.monster.literaryflow.GalleryActivity
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.photoScreen.ScreenActivity
import com.monster.literaryflow.service.ScreenFloatingWindowsService
import com.monster.literaryflow.service.TextFloatingWindowService


class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GridViewExample(this)
        }
    }
}

@Composable
fun GridViewExample(context: Context) {

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    MyApp.image.observeForever {
        bitmap = it
    }
    val items = remember { mutableStateListOf<Item>() }
    items.add(Item("图片识别测试"){
        context.startActivity(Intent(context, GalleryActivity::class.java))
    })
    items.add(Item("截屏测试"){
        context.startActivity(Intent(context, ScreenActivity::class.java))
    })

    // 获取Context
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen ImageView
        bitmap?.let {
            Image(
                bitmap =it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier.fillMaxSize()
            )
        }
        // UI Layout
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBarExample()
            //设置顶部间距
            Text(text = "请选择功能", modifier = Modifier.padding(16.dp))

            Button(
                onClick = {
                    val intent = Intent(context, ScreenFloatingWindowsService::class.java)
                    // 对于 Android O 及以上，建议使用 startForegroundService
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    } },
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
                Text("坐标截屏测试", color = Color.White)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2 columns in the grid
                modifier = Modifier.fillMaxSize()
            ) {
                items(items.size) { index ->
                    ItemView(item = items[index])
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarExample() {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("测试模块") },
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
// Data class to represent an item
data class Item(val name: String, val onClick: () -> Unit)
@Composable
fun ItemView(item: Item) {
    Text(
        text = item.name,
        modifier = Modifier
            .padding(8.dp)
            .clickable { item.onClick() }
    )
}




