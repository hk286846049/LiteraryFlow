package com.monster.literaryflow.utils

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.app.Activity
import android.view.View

object KeyboardUtil {

    // 隐藏软键盘
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // 隐藏软键盘（如果没有焦点的视图）
    fun hideKeyboard(context: Context, view: View) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
