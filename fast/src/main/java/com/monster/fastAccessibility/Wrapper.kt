package cn.coderpig.cp_fast_accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

data class EventWrapper(
    var packageName: String,
    var className: String,
    var eventType: Int
) {
    override fun toString() = "$packageName → $className → $eventType"
}


/**
 * 视图结点包装类
 * */
data class NodeWrapper(
    var index: Int? = null,
    var text: String? = null,
    var id: String? = null,
    var bounds: Rect? = null,
    var className: String,
    var description: String? = null,
    var clickable: Boolean = false,
    var scrollable: Boolean = false,
    var editable: Boolean = false,
    var nodeInfo: AccessibilityNodeInfo? = null
) {
    //    override fun toString() = "$index → $className → $text → $id → $description → ${bounds!!.centerX()},${bounds!!.centerY()} → $clickable → $scrollable → $editable"
    override fun toString() = if (text == null || text=="") {
        ""
    } else {
        "[$text]${bounds?.centerX()},${bounds?.centerY()}"
    }
}


data class AnalyzeSourceResult(
    var nodes: ArrayList<NodeWrapper> = arrayListOf()
)