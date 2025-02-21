package com.monster.literaryflow.autoRun.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.monster.literaryflow.R
import com.monster.literaryflow.databinding.ActivityAddAutoBinding
import com.monster.literaryflow.databinding.ActivityAddMonitorBinding
import com.monster.literaryflow.utils.KeyboardUtil

class AddMonitorActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAddMonitorBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        KeyboardUtil.hideKeyboard(this)
    }
}