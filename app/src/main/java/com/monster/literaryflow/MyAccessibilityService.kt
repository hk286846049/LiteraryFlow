package com.monster.literaryflow

import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import cn.coderpig.cp_fast_accessibility.*
import com.monster.fastAccessibility.FastAccessibilityService
import com.monster.literaryflow.bean.NodeBean

class MyAccessibilityService : FastAccessibilityService() {
    companion object {
        var status = 0  // 1循环滑动

        @SuppressLint("WrongConstant")
        fun clickNode(ocrBlock: ArrayList<com.benjaminwan.ocrlibrary.TextBlock>) {
            /*
                        if (MyApp.image!=null){
                            if (ScreenUtils.isLandscape(MyApp.instance)) {
                                ImageUtils.saveImageToGallery(MyApp.image!!)
                            }else{
                            }
                        }

            */

            when (status) {
                0 -> {
                    clickRule(ocrBlock)
                }

                1 -> {

                }
            }

        }

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
                    mutableListOf( "向上滑动浏览落地页", "领取成功"),
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
                    exportString = mutableListOf( "领福利")
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

    }


    fun swipe() {

    }

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        if (wrapper?.packageName == "com.ss.android.article.lite") {
        }
    }

    override fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) {
        wrapper?.let { logD(it.toString()) }
        //头条极速版
        if (wrapper?.packageName == "com.ss.android.article.lite") {
            node.printAllNode()
        }
    }

}