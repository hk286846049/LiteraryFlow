package com.monster.literaryflow.bean

import java.io.Serializable

data class ClickBean(
    var clickType: RunType? = null,
    //X Y 坐标
    var clickXy: Pair<Int, Int> = Pair(0, 0),
    var scrollMinMax: Pair<Int, Int> = Pair(0, 0),
    var text: String? = null,
    var findTextTime: Int = 1,
    var sleepTime: Int = 1,
    var loopTimes: Int = 1,
    var scrollTime: Int = 15,
    var findTextType: TextPickType = TextPickType.EXACT_MATCH,
    var enterText: String? = null,
    var runTask: Pair<String, MutableList<RunBean>?>? = null,
    //pair(appName,packageName)
    var openAppData: Pair<String, String>? = null,
    var isFindText4Node: Boolean = true,
    var longClickTime: Int = 1
) : Serializable
