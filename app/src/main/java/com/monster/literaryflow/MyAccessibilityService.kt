package com.monster.literaryflow

import android.view.accessibility.AccessibilityNodeInfo
import cn.coderpig.cp_fast_accessibility.*

class MyAccessibilityService : FastAccessibilityService() {
    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        if (wrapper?.packageName == "com.ss.android.article.lite") {
            if (wrapper?.className == ""){
                
            }
            val node = result.findNodeByText(
                "未登录",
                textAllMatch = false,
                includeDesc = false,
                descAllMatch = false,
                enableRegular = false
            )
            logD("analyzeCallBack - node : $node")
            node.click(false)
        }
    }

    override fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) {
        wrapper?.let { logD(it.toString()) }
        //头条极速版
        if (wrapper?.packageName == "com.ss.android.article.lite") {
            node.printAllNode()
        }
    }
}