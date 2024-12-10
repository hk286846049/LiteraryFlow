package com.monster.literaryflow.utils

import cn.coderpig.cp_fast_accessibility.clickA
import cn.coderpig.cp_fast_accessibility.sleep
import com.benjaminwan.ocrlibrary.TextBlock

object ClickXY {
    fun openBossMap(millis: Long) {
        clickA(1399, 233)
        sleep(millis)
    }

    fun closeBossMap(millis: Long) {
        clickA(1632, 155)
        sleep(millis)
    }
    fun openMap(millis: Long) {
        clickA(1759, 188)
        sleep(millis)
    }

    fun closeMap(millis: Long) {
        clickA(1648, 157)
        sleep(millis)
    }

    fun autoPlay(millis: Long) {
        clickA(1731, 469)
        sleep(millis)
    }

    fun exitKf() {
        clickA(499, 319)
        sleep(1000)
        clickA(1093, 644)
        sleep(5000)
    }
    fun goDb() {
        clickA(1522, 210)
        sleep(1000)
        clickA(1093, 644)
        sleep(5000)
    }

    fun click4Str(
        str: String,
        millis: Long,
        ocrBlock: ArrayList<TextBlock>
    ): Boolean {
        return if (ocrBlock.find { it.text.contains(str) } != null) {
            val node = ocrBlock.find { it.text.contains(str) }
            val maxX = node!!.boxPoint.maxBy { it.x }
            val maxY = node!!.boxPoint.maxBy { it.y }
            val minX = node!!.boxPoint.minBy { it.x }
            val minY = node!!.boxPoint.minBy { it.y }
            clickA((maxX.x + minX.x) / 2, (maxY.y + minY.y) / 2)
            sleep(millis)
            true
        } else false
    }
    fun click4StrEqual(
        str: String,
        millis: Long,
        ocrBlock: ArrayList<TextBlock>
    ): Boolean {
        return if (ocrBlock.find { it.text==str } != null) {
            val node = ocrBlock.find {it.text==str }
            val maxX = node!!.boxPoint.maxBy { it.x }
            val maxY = node!!.boxPoint.maxBy { it.y }
            val minX = node!!.boxPoint.minBy { it.x }
            val minY = node!!.boxPoint.minBy { it.y }
            clickA((maxX.x + minX.x) / 2, (maxY.y + minY.y) / 2)
            sleep(millis)
            true
        } else false
    }
    //1080 1920
    //1260 2800


}