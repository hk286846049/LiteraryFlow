package com.monster.literaryflow

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import cn.coderpig.cp_fast_accessibility.getDrawableRes
import cn.coderpig.cp_fast_accessibility.getStringRes
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.requireAccessibility
import cn.coderpig.cp_fast_accessibility.shortToast
import cn.coderpig.cp_fast_accessibility.startApp
import com.monster.literaryflow.databinding.ActivityMainBinding
import com.monster.literaryflow.databinding.ActivityScreenBinding
import com.monster.literaryflow.photoScreen.ScreenActivity

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var mServiceStatusIv: ImageView
    private lateinit var mServiceStatusTv: TextView
    private lateinit var mOpenTargetAppBt: Button
    private lateinit var mScreen: TextView
    private val mClickListener = View.OnClickListener {
        when (it.id) {
            R.id.iv_service_status -> {
                if (isAccessibilityEnable) shortToast(getStringRes(R.string.service_is_enable_tips))
                else requireAccessibility()
            }

            R.id.bt_open_target_app -> {
                startApp("com.ss.android.article.lite", "com.ss.android.article.lite.activity.SplashActivity", "未安装呢")
            }
            R.id.tv_screen ->{
                startActivity(Intent(this@MainActivity,ScreenActivity::class.java))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        mServiceStatusIv = findViewById(R.id.iv_service_status)
        mServiceStatusTv = findViewById(R.id.tv_service_status)
        mOpenTargetAppBt = findViewById(R.id.bt_open_target_app)
        mScreen = findViewById(R.id.tv_screen)
        mServiceStatusIv.setOnClickListener(mClickListener)
        mOpenTargetAppBt.setOnClickListener(mClickListener)
        mScreen.setOnClickListener(mClickListener)
        binding.tvOcr.setOnClickListener {
            startActivity(Intent(this@MainActivity,GalleryActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnable) {
            mServiceStatusIv.setImageDrawable(getDrawableRes(R.drawable.ic_service_enable))
            mServiceStatusTv.text = getStringRes(R.string.service_status_enable)
            mOpenTargetAppBt.visibility = View.VISIBLE
//            FastAccessibilityService.showForegroundNotification(
//                "守护最好的坤坤🐤", "用来保护的，不用理我", "提示信息",
//                activityClass = MainActivity::class.java
//            )
        } else {
            mServiceStatusIv.setImageDrawable(getDrawableRes(R.drawable.ic_service_disable))
            mServiceStatusTv.text = getStringRes(R.string.service_status_disable)
            mOpenTargetAppBt.visibility = View.GONE
        };
    }
}