package com.monster.literaryflow.bean

import java.io.Serializable

data class RunBean(
    var clickBean: ClickBean? = null,
    var triggerBean: TriggerBean? = null,
) : Serializable

enum class RuleType {
    FIND_TEXT, TIME
}

enum class RunType {
    LONG_HOR, LONG_VEH, CLICK_XY, CLICK_TEXT, SCROLL_LEFT, SCROLL_RIGHT, SCROLL_TOP, SCROLL_BOTTOM, ENTER_TEXT, GO_BACK, TASK, OPEN_APP,LONG_CLICK
}

enum class TextPickType {
    EXACT_MATCH, FUZZY_MATCH, MULTIPLE_FUZZY_WORDS
}