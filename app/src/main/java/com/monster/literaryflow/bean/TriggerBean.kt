package com.monster.literaryflow.bean

import java.io.Serializable

data class TriggerBean(
    var triggerType: RuleType? = null,
    //查找文字
    var findText: String? = null,
    //运行时间区间
    var runTime: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
    //判断时间
    var runScanTime:Int = 60,
    //满足时执行任务
    var runTrueAuto: Pair<String,MutableList<RunBean>?>? = null,
    var runTrueTask:ClickBean? = null,
    //不满足时执行任务
    var runFalseAuto:Pair<String,MutableList<RunBean>?>? = null,
    var runFalseTask:ClickBean? = null,
    var findTextType:TextPickType = TextPickType.EXACT_MATCH,
    var isFindText4Node :Boolean =true,
): Serializable