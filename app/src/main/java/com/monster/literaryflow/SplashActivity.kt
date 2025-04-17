package com.monster.literaryflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation_lottie))
            val animationState = animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                isPlaying = true,
                speed = 0.8f,
                restartOnPlay = false
            )
            LaunchedEffect(animationState.isPlaying) {
                if (!animationState.isPlaying && animationState.progress == 1f) {
                    showSplash = false
                }
            }

            if (showSplash) {
                LottieAnimation(
                    composition = composition,
                    progress = { animationState.progress },
                )
            } else {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }
    }

}