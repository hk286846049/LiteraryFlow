package com.monster.literaryflow

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.media.ImageReader
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import cn.coderpig.cp_fast_accessibility.*
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.utils.ScreenUtils
import kotlin.random.Random

class MyAccessibilityService : FastAccessibilityService() {
    companion object{
        @SuppressLint("WrongConstant")
        fun clickNode(ocrBlock: ArrayList<com.benjaminwan.ocrlibrary.TextBlock>){
/*
            if (MyApp.image!=null){
                if (ScreenUtils.isLandscape(MyApp.instance)) {
                    ImageUtils.saveImageToGallery(MyApp.image!!)
                }else{
                }
            }
*/
            if (ocrBlock.find { it.text=="适龄提示" }!=null){
                val node =  ocrBlock.find { it.text=="适龄提示" }
                Log.d("~~~", "node：${node}")
                if (node!=null){
                    ImageUtils.saveImageToGallery(MyApp.image!!)
                    val maxX = node.boxPoint.maxBy { it.x }
                    val maxY= node.boxPoint.maxBy { it.y }
                    val minX = node.boxPoint.minBy { it.x }
                    val minY= node.boxPoint.minBy { it.y }
//                    clickA(minX.x,minY.y,maxX.x,maxY.y)
                    clickA(1422,912,1433,933)
                    Log.d("~~~", "click：${minX.x}, ${minY.y}, ${maxX.x}, ${maxY.y}")
                }
            }


        }
    }
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
    /**
     * 使用手势模拟点击，长按的话，传入的Duration大一些就好，比如1000(1s)
     *
     * @param x 点击坐标点的x坐标
     * @param y 点击坐标点的y坐标
     * @param delayTime 延迟多久进行本次点击，单位毫秒
     * @param duration 模拟触摸屏幕的时长(按下到抬起)，太短会导致部分应用下点击无效，单位毫秒
     * @param repeatCount 本次点击重复多少次，必须大于0
     * @param randomTime 在随机参数上加减延时时长，有助于防止点击器检测，单位毫秒
     *
     * */
    fun clickA(
        minX: Int,
        minY: Int,
        randomPosition: Int = 10,
        delayTime: Long = 0,
        duration: Long = 200,
        repeatCount: Int = 1,
        randomTime: Long = 100
    ) {
        repeat(repeatCount) {
            val tempX = minX + Random.nextInt(0 - randomPosition, randomPosition + 1)
            val tempY = minY + Random.nextInt(0 - randomPosition, randomPosition + 1)
            val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
            FastAccessibilityService.require?.dispatchGesture(
                fastGestureDescription({
                    it.moveTo(
                        tempX.toFloat(),
                        tempY.toFloat()
                    )
                }, delayTime, tempDuration), fastGestureCallback(), null
            )
        }
    }
}