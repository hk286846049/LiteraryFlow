package com.monster.literaryflow.rule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.monster.literaryflow.rule.ui.theme.LiteraryFlowTheme
import com.monster.literaryflow.utils.AppUtils


class ComposeAppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = AppUtils.getAllInstalledApps(this@ComposeAppListActivity)
        setContent {
            LiteraryFlowTheme {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        repeat(apps.size){
                            item(key =apps[it].appName) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = rememberImagePainter(AppUtils.getAppIcon(this@ComposeAppListActivity, apps[it].packageName)),
                                        modifier = Modifier
                                            .size(60.dp, 60.dp)
                                            .background(Color.Yellow),
                                        contentDescription = "content description"
                                    )
                                    Text(text = apps[it].appName)
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    LiteraryFlowTheme {
        Greeting2("Android")
    }
}