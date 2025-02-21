package com.monster.literaryflow.serviceInterface

import cn.coderpig.cp_fast_accessibility.NodeWrapper
import com.monster.literaryflow.bean.TextPickType

interface ServiceInterface {
    //查找是否有此文字
    fun findText(text:String):Pair<Boolean,ArrayList<NodeWrapper>?>
    //查找此文字并点击
    fun clickText(text:String,type: TextPickType):Boolean
    //在输入框中输入此文字
    fun enterText(input:String):Boolean
}