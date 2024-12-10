package com.monster.literaryflow

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.media.ImageReader
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import cn.coderpig.cp_fast_accessibility.*
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.Const.Companion.JrPackage
import com.monster.literaryflow.Const.Companion.KgPackage
import com.monster.literaryflow.bean.NodeBean
import com.monster.literaryflow.photoScreen.ImageUtils
import com.monster.literaryflow.rule.CqRule
import com.monster.literaryflow.utils.ScreenUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class MyAccessibilityService : FastAccessibilityService() {
    companion object {
        var status = 0  // 1循环滑动
        private val connectionMutex = Mutex()
        @SuppressLint("WrongConstant")
        suspend fun clickNode(ocrBlock: ArrayList<com.benjaminwan.ocrlibrary.TextBlock>) {
            if (connectionMutex.isLocked){
                return
            }
            connectionMutex.withLock {
                when (MyApp.runRuleApp) {
                    JrPackage -> {
                        when (status) {
                            0 -> {
                                clickRule(ocrBlock)
                            }

                            1 -> {

                            }
                        }
                    }

                    KgPackage -> {
                        kgRule(ocrBlock)
                    }

                    Const.CqPackage ->{
                        CqRule.init(ocrBlock)
                    }
                }

            }


        }

        var tPackageName: String? = null

        private fun clickRule(ocrBlock: ArrayList<com.benjaminwan.ocrlibrary.TextBlock>) {
            //开宝箱
            /*      val nodeList = mutableListOf<NodeBean>()
                  nodeList.add(NodeBean(mutableListOf("收益快报"),"不再提示"))
                  nodeList.add(NodeBean(mutableListOf("关注","推荐","发现","首页","任务"),"",true,526,2741))
                  nodeList.add(NodeBean(mutableListOf("7天签到必得5元","点击领取"),"点击领取"))
                  nodeList.add(NodeBean(mutableListOf("7天签到必得5元","立即提现"),"",true,638,2219))
                  nodeList.add(NodeBean(mutableListOf("任务","逛街","搜一搜赚钱","开宝箱得金币"),"开宝箱得金币"))
                  nodeList.add(NodeBean(mutableListOf("恭喜获得宝箱奖励","看视频再领"),"看视频再领"))
                  nodeList.add(NodeBean(mutableListOf("恭喜已获得","看视频再得"),"看视频再得"))
                  nodeList.add(NodeBean(mutableListOf("恭喜你获得","开心收下"),"开心收下"))
                  nodeList.add(NodeBean(mutableListOf("添加赚钱助手得","立即添加领金币"),"",true,1193,1481))
                  nodeList.add(NodeBean(mutableListOf("猜你想搜","彩票","天气","日历"),"",true,368,726))
                  nodeList.add(NodeBean(mutableListOf("广告","向上滑动浏览落地页","领取成功"),"领取成功"))
                  nodeList.add(NodeBean(mutableListOf("恭喜获得搜索提现特权","立即提现"),"",true,647,1989))*/

            //看广告
            val nodeList = mutableListOf<NodeBean>()
            nodeList.add(NodeBean(mutableListOf("收益快报"), "不再提示"))
            nodeList.add(
                NodeBean(
                    mutableListOf("关注", "推荐", "发现", "首页", "任务"),
                    "",
                    true,
                    526,
                    2741
                )
            )
            nodeList.add(NodeBean(mutableListOf("恭喜已获得", "看视频再得"), "看视频再得"))
            nodeList.add(NodeBean(mutableListOf("恭喜你获得", "开心收下"), "开心收下"))
            nodeList.add(NodeBean(mutableListOf("7天签到必得5元", "点击领取"), "点击领取"))
            nodeList.add(NodeBean(mutableListOf("7天签到必得5元", "立即提现"), "", true, 638, 2219))
            nodeList.add(NodeBean(mutableListOf("恭喜获得宝箱奖励", "看视频再领"), "看视频再领"))


            nodeList.add(
                NodeBean(
                    mutableListOf("添加赚钱助手得", "立即添加领金币"),
                    "",
                    true,
                    1193,
                    1481
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("猜你想搜", "彩票", "天气", "日历"),
                    "",
                    true,
                    368,
                    726
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("向上滑动浏览落地页", "领取成功"),
                    "领取成功",
                    exportString = mutableListOf("开心收下", "看视频再得")
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("广告", "立即下载，免费阅读全文", "领取成功"),
                    "领取成功",
                    exportString = mutableListOf("开心收下", "看视频再得")
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("广告", "极速下载", "领取成功"),
                    "领取成功",
                    exportString = mutableListOf("开心收下", "看视频再得")
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("立即下载", "领取成功"),
                    "领取成功",
                    exportString = mutableListOf("开心收下", "看视频再得")
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("恭喜获得搜索提现特权", "立即提现"),
                    "",
                    true,
                    647,
                    1989
                )
            )
            nodeList.add(NodeBean(mutableListOf("看广告赚金币", "领福利"), "看广告赚金币"))
            nodeList.add(
                NodeBean(
                    mutableListOf("任务", "逛街", "开宝箱得金币"),
                    "开宝箱得金币",
                    exportString = mutableListOf("领福利")
                )
            )


            //去阅读
            /*     val nodeList = mutableListOf<NodeBean>()
                 nodeList.add(NodeBean(mutableListOf("收益快报"),"不再提示"))
                 nodeList.add(NodeBean(mutableListOf("关注","推荐","发现","首页","任务"),"",true,526,2741))
                 nodeList.add(NodeBean(mutableListOf("7天签到必得5元","点击领取"),"点击领取"))
                 nodeList.add(NodeBean(mutableListOf("7天签到必得5元","立即提现"),"",true,638,2219))
                 nodeList.add(NodeBean(mutableListOf("恭喜获得宝箱奖励","看视频再领"),"看视频再领"))
                 nodeList.add(NodeBean(mutableListOf("恭喜已获得","看视频再得"),"看视频再得"))
                 nodeList.add(NodeBean(mutableListOf("恭喜你获得","开心收下"),"开心收下"))
                 nodeList.add(NodeBean(mutableListOf("添加赚钱助手得","立即添加领金币"),"",true,1193,1481))
                 nodeList.add(NodeBean(mutableListOf("猜你想搜","彩票","天气","日历"),"",true,368,726))
                 nodeList.add(NodeBean(mutableListOf("广告","向上滑动浏览落地页","领取成功"),"领取成功"))
                 nodeList.add(NodeBean(mutableListOf("恭喜获得搜索提现特权","立即提现"),"",true,647,1989))
                 nodeList.add(NodeBean(mutableListOf("看文章或视频赚金币","去阅读"),"去阅读"))*/
            nodeList.forEach { nodeBean ->
                var isHas = true
                nodeBean.titleNames.forEach { titleName ->
                    if (ocrBlock.find { it.text.contains(titleName) } == null) {
                        isHas = false
                    } else {
                        if (nodeBean.exportString != null) {
                            nodeBean.exportString.forEach { exportStr ->
                                if (ocrBlock.find { it.text.contains(exportStr) } != null) {
                                    isHas = false
                                }
                            }
                        }
                    }
                }
                if (isHas) {
                    if (nodeBean.isXy) {
                        clickA(nodeBean.x, nodeBean.y, nodeBean.x + 2, nodeBean.y + 2)
                        Log.d(
                            "~~~",
                            "click：${nodeBean.x}, ${nodeBean.y}, ${nodeBean.x + 2}, ${nodeBean.y + 2}"
                        )

                    } else {
                        val node = ocrBlock.find { it.text.contains(nodeBean.clickName) }
                        val maxX = node!!.boxPoint.maxBy { it.x }
                        val maxY = node!!.boxPoint.maxBy { it.y }
                        val minX = node!!.boxPoint.minBy { it.x }
                        val minY = node!!.boxPoint.minBy { it.y }
                        clickA(minX.x, minY.y, maxX.x, maxY.y)
                        Log.d("~~~", "click：${minX.x}, ${minY.y}, ${maxX.x}, ${maxY.y}")

                    }
                    return
                }
            }
        }

        private fun kgRule(ocrBlock: ArrayList<com.benjaminwan.ocrlibrary.TextBlock>) {

            val nodeList = mutableListOf<NodeBean>()
            nodeList.add(NodeBean(mutableListOf("下载资源", "取消", "确认"), "确认"))
            nodeList.add(NodeBean(mutableListOf("发生错误", "取消", "确认"), "确认"))
            nodeList.add(NodeBean(mutableListOf("确认", "领取奖励"), "确认"))
            nodeList.add(NodeBean(mutableListOf("签到", "领取奖励"), "领取奖励"))
            nodeList.add(NodeBean(mutableListOf("适龄提示"), "", false, 1401, 990))

            nodeList.add(
                NodeBean(
                    mutableListOf("活动公告", "系统公告"),
                    "",
                    true,2468, 173
                )
            )
            nodeList.add(
                NodeBean(
                    mutableListOf("推荐商品","2周内不再提示"),
                    "",
                    true,717, 1133
                )
            )

            nodeList.forEach { nodeBean ->
                var isHas = true
                nodeBean.titleNames.forEach { titleName ->
                    if (ocrBlock.find { it.text.contains(titleName) } == null) {
                        isHas = false
                    } else {
                        if (nodeBean.exportString != null) {
                            nodeBean.exportString.forEach { exportStr ->
                                if (ocrBlock.find { it.text.contains(exportStr) } != null) {
                                    isHas = false
                                }
                            }
                        }
                    }
                }
                if (isHas) {
                    if (nodeBean.isXy) {
                        clickA(nodeBean.x, nodeBean.y, nodeBean.x + 2, nodeBean.y + 2)
                        Log.d(
                            "~~~",
                            "click：${nodeBean.x}, ${nodeBean.y}, ${nodeBean.x + 2}, ${nodeBean.y + 2}"
                        )

                    } else {
                        val node = ocrBlock.find { it.text.contains(nodeBean.clickName) }
                        val maxX = node!!.boxPoint.maxBy { it.x }
                        val maxY = node!!.boxPoint.maxBy { it.y }
                        val minX = node!!.boxPoint.minBy { it.x }
                        val minY = node!!.boxPoint.minBy { it.y }
                        clickA(minX.x, minY.y, maxX.x, maxY.y)
                        Log.d("~~~", "click：${minX.x}, ${minY.y}, ${maxX.x}, ${maxY.y}")
                    }
                    return
                }
            }
        }

    }

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        tPackageName = wrapper?.className
        if (wrapper?.packageName == "com.ss.android.article.lite") {
        }
    }

    override fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) {
        wrapper?.let { logD(it.toString()) }
        //头条极速版
        /*  if (wrapper?.packageName == "com.ss.android.article.lite") {
              node.printAllNode()
          }*/
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