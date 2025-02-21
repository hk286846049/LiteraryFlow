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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.monster.literaryflow.GalleryActivity
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.photoScreen.ScreenActivity


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
            // LazyVerticalGrid to display items
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




