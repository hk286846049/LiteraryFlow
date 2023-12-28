package com.monster.literaryflow

import android.app.Application
import android.view.accessibility.AccessibilityEvent
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService


class MyApp : Application() {
    companion object {
        lateinit var instance: Application
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FastAccessibilityService.init(
            instance, MyAccessibilityService::class.java, arrayListOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_CLICKED,
            )
        )
    }
}