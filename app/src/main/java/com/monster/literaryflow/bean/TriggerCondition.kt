package com.monster.literaryflow.bean

// 新增触发条件数据类
data class TriggerCondition(
    val type: TriggerType,
    val targetText: String? = null,
    val viewId: String? = null,
    val checkInterval: Long = 1000L
)

// 触发类型枚举
enum class TriggerType {
    TEXT_APPEAR,    // 文本出现
    VIEW_VISIBLE    // 控件可见
}