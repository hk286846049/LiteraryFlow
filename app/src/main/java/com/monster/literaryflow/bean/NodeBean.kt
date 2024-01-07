package com.monster.literaryflow.bean

data class NodeBean(
    val titleNames: MutableList<String>,
    val clickName: String,
    val isXy: Boolean = false,
    val x: Int = 0,
    val y: Int = 0,
    val clickType:Int = 1,//1 点击 2 滑动 3 返回
    val exportString:MutableList<String>? = null
) {
}