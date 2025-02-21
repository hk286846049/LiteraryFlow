package com.monster.literaryflow.rule

import android.content.SharedPreferences
import android.os.Build
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.clickA
import cn.coderpig.cp_fast_accessibility.sleep
import cn.coderpig.cp_fast_accessibility.swipe
import com.benjaminwan.ocrlibrary.TextBlock
import com.monster.literaryflow.MyApp
import com.monster.literaryflow.utils.AppUtils
import com.monster.literaryflow.utils.ClickXY
import com.monster.literaryflow.utils.TimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

object CqRule {
    //活动地图？
    var isHuodong = false

    //跨服boss剩余次数
    var kfBossTimes = 2

    // 当前流程
    var isRun = false

    //当前执行类型
    var runType = ""
    val KFBOSS = "KFBOSS"

    //当前步骤
    var runStep = 0

    // 上次停留时间
    var lastRunTime = 0L

    //尝试次数
    var retryTimes = 5

    var jdBossTime = 0L
    var enterJdTime = 0L

    //魔痕上次时间
    var mhTime = 0L

    //魔痕检查类型
    var mhType = 1

    // 是否魔痕地图
    var isMhMap = false

    //名剑次数
    var mjTimes = 5
    var mjBossTime = 0L
    var isMjMap = false
    var killBossType = 0

    // 是否gs
    var isGs = false
    var scX = mutableListOf(814, 789, 828, 857)
    var scY = mutableListOf(413, 395, 402, 390)
    var scPoi = 0
    var isDie = false
    var reLineTime = 0L

    fun init(ocrBlock: ArrayList<TextBlock>) {
        var hour = 0
        var minute = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentTime = LocalTime.now()
            hour = currentTime.hour
            minute = currentTime.minute
        } else {
            val calendar = Calendar.getInstance()
            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minute = calendar.get(Calendar.MINUTE)
        }
        login(ocrBlock)
        if (ClickXY.click4Str("回城复活", 4000, ocrBlock)) {
            isDie = true
            return
        }
        if (ClickXY.click4Str("领取奖励", 1000, ocrBlock)) {
            return
        }
        if ((ocrBlock.find { it.text.contains("打宝") || it.text.contains("激战") } != null) &&
            (ocrBlock.none {
                it.text.contains("活动") || it.text.contains("礼包") || it.text.contains(
                    "打金"
                )
            })
        ) {
            clickA(1625, 42)
            sleep(1000)
            return
        }
        if (isGs) {
            ClickXY.click4Str("领取", 1000, ocrBlock)
            val node1 = ocrBlock.find {
                it.text.contains("地图")
            }
            if (node1 == null) {
                ClickXY.openMap(1000)
            }
            clickA(scX[scPoi], scY[scPoi])
            if (scPoi == 3) {
                scPoi = 0
                ClickXY.closeMap(1000)
            } else {
                scPoi++
            }
            sleep(5000)
            return
        }
        if (isMjMap) {
            val nodeMap = ocrBlock.find {
                it.text.contains("地图")
            }
            if (nodeMap == null) {
                val timesNode =
                    ocrBlock.find { it.text.contains("剩余") || it.text.contains("次数") }
                if (timesNode != null) {
                    if (timesNode.text.contains("0")) {
                        mjTimes = 0
                        ClickXY.closeMap(1000)
                        clickA(469, 327)
                        sleep(1000)
                        clickA(1093, 644)
                        sleep(3000)
                        isMjMap = false
                        reset()
                        mjBossTime = System.currentTimeMillis()
                        return
                    }
                }
                ClickXY.openMap(1000)
                return
            }

            val bossNode = ocrBlock.find { it.text.contains("存活") }
            if (bossNode != null) {
                retryTimes = 5
                ClickXY.closeMap(1000)
                if (isDie) {
                    isDie = false
                    ClickXY.autoPlay(4000)
                }else{
                    if (ocrBlock.find { it.text.contains("自动") } == null) {
                        ClickXY.autoPlay(2000)
                    }
                }
            } else {
                if (retryTimes > 0) {
                    retryTimes--
                    sleep(1000)
                } else {
                    ClickXY.closeMap(1000)
                    clickA(469, 327)
                    sleep(1000)
                    clickA(1093, 644)
                    sleep(3000)
                    isMjMap = false
                    reset()
                    mjBossTime = System.currentTimeMillis()
                }
            }
          /*  val bossA = hasStr(mutableListOf("尚", "存", "活"), 677, 502, ocrBlock)
            val bossB = hasStr(mutableListOf("尚", "存", "活"), 796, 523, ocrBlock)
            val bossC = hasStr(mutableListOf("尚", "存", "活"), 967, 518, ocrBlock)
            if (bossA == null && bossB == null && bossC == null) {
                ClickXY.closeMap(1000)
                clickA(469, 327)
                sleep(1000)
                clickA(1093, 644)
                sleep(3000)
                isMjMap = false
                reset()
                mjBossTime = System.currentTimeMillis()
            } else {
                if (bossA != null) {
                    if ((killBossType == 1 && !isDie)) {
                        if (ocrBlock.find { it.text.contains("自动") } == null) {
                            ClickXY.autoPlay(1000 * 10)
                        } else sleep(1000 * 10)
                    } else {
                        killBossType = 1
                        isDie = false
                        clickA(677, 502)
                        sleep(1000)
                        ClickXY.closeMap(10000)
                        ClickXY.autoPlay(10000)
                    }
                } else if (bossB != null) {
                    if ((killBossType == 2 && !isDie)) {
                        if (ocrBlock.find { it.text.contains("自动") } == null) {
                            ClickXY.autoPlay(1000 * 10)
                        } else sleep(1000 * 10)
                    } else {
                        killBossType = 2
                        clickA(796, 523)
                        sleep(1000)
                        ClickXY.closeMap(1000)
                        ClickXY.autoPlay(10000)
                        isDie = false
                    }
                } else if (bossC != null) {
                    if ((killBossType == 3 && !isDie)) {
                        if (ocrBlock.find { it.text.contains("自动") } == null) {
                            ClickXY.autoPlay(1000 * 10)
                        } else sleep(1000 * 10)
                    } else {
                        killBossType = 3
                        clickA(967, 518)
                        sleep(1000)
                        ClickXY.closeMap(10000)
                        ClickXY.autoPlay(10000)
                        isDie = false
                    }
            }
                }*/
            return

        }
/*
        if (isMhMap) {
            val nodeMap = ocrBlock.find {
                it.text.contains("地图")
            }
            if (nodeMap == null) {
                ClickXY.openMap(1000)
                return
            }
            val bossNode = ocrBlock.find { it.text.contains("存活") }
            if (bossNode != null) {
                retryTimes = 5
                ClickXY.closeMap(1000)
                if (isDie) {
                    isDie = false
                    ClickXY.autoPlay(4000)
                }else{
                    if (ocrBlock.find { it.text.contains("自动") } == null) {
                        ClickXY.autoPlay(2000)
                    }
                }
            } else {
                if (retryTimes > 0) {
                    retryTimes--
                    sleep(1000)
                } else {
                    ClickXY.closeMap(1000 * 40)
                    ClickXY.exitKf()
                    reset()
                    isMhMap = false
                    mhTime = System.currentTimeMillis()
                }
            }
            return


        }
*/

        if (ocrBlock.find { it.text == "每日特惠" } != null) {
            clickA(1629, 154)
            sleep(1000)
            reset()
            return
        }
        if ((hour == 12 && minute == 11) || (hour == 19 && minute == 46) || (hour == 20 && minute == 16) || (hour == 21 && minute == 31)) {
            reset()
        }
        if (minute > 57 && isMhMap) {
            val node1 = ocrBlock.find {
                it.text.contains("地图")
            }
            if (node1 != null) {
                ClickXY.closeMap(1000)
            }
            ClickXY.exitKf()
            reset()
            mhTime = System.currentTimeMillis()
            return
        }

        //跨服3次
        kf(hour, minute, ocrBlock)
        //12点 19点  活动
        if ((hour == 12 && minute in 0..11) || (hour == 19 && minute in 30..41)) {
            if (ocrBlock.find { it.text == "点击前往" } != null && runStep == 0) {
                runStep = 1
                val node = ocrBlock.find { it.text == "点击前往" }
                val maxX = node!!.boxPoint.maxBy { it.x }
                val maxY = node.boxPoint.maxBy { it.y }
                val minX = node.boxPoint.minBy { it.x }
                val minY = node.boxPoint.minBy { it.y }
                clickA(minX.x, minY.y, maxX.x, maxY.y)
                sleep(4000)
            } else if (runStep == 0) {
                runStep = 1
                clickA(1255, 84)
                sleep(4000)
            }
            if (ocrBlock.find { it.text == "前往" } != null && runStep == 1) {
                runStep = 2
                val node = ocrBlock.find { it.text == "前往" }
                val maxX = node!!.boxPoint.maxBy { it.x }
                val maxY = node!!.boxPoint.maxBy { it.y }
                val minX = node!!.boxPoint.minBy { it.x }
                val minY = node!!.boxPoint.minBy { it.y }
                clickA(minX.x, minY.y, maxX.x, maxY.y)
                sleep(3000)
            }
            //挂机
            if (runStep == 2) {
                if ((hour ==12 &&minute>=11 )||(hour ==19 &&minute>=41)){
                    clickA(867, 873)
                    reset()
                }
                if (ocrBlock.find { it.text.contains("自动") } == null) {
                    ClickXY.autoPlay(2000)
                }
            }
        }
        //8点活动
        huodong8(hour, minute, ocrBlock)
        //9点活动
        huodong9(hour, minute, ocrBlock)
        //魔痕
//        mh(hour, minute, ocrBlock)
    }

    private fun huodong8(hour: Int, minute: Int, ocrBlock: ArrayList<TextBlock>) {
        if ((hour == 20 && minute in 0..10)) {
            if (ocrBlock.find { it.text == "点击前往" } != null && runStep == 0) {
                runStep = 1
                val node = ocrBlock.find { it.text == "点击前往" }
                val maxX = node!!.boxPoint.maxBy { it.x }
                val maxY = node.boxPoint.maxBy { it.y }
                val minX = node.boxPoint.minBy { it.x }
                val minY = node.boxPoint.minBy { it.y }
                clickA(minX.x, minY.y, maxX.x, maxY.y)
                sleep(4000)
            } else if (runStep == 0) {
                runStep = 1
                clickA(1255, 84)
                sleep(4000)
            }
            if (ocrBlock.find { it.text == "前往" } != null && runStep == 1) {
                runStep = 2
                val node = ocrBlock.find { it.text == "前往" }
                val maxX = node!!.boxPoint.maxBy { it.x }
                val maxY = node!!.boxPoint.maxBy { it.y }
                val minX = node!!.boxPoint.minBy { it.x }
                val minY = node!!.boxPoint.minBy { it.y }
                clickA(minX.x, minY.y, maxX.x, maxY.y)
                sleep(3000)
            }

            if (runStep == 2) {
                clickA(1146, 882)
                sleep(6000)
                runStep = 3
                isHuodong = true
            }
            //挂机
            if (runStep == 3) {
                if (ocrBlock.find { it.text.contains("自动") } == null) {
                    ClickXY.autoPlay(1000 * 60 * 10)
                    for (a in 0..10) {
                        clickA(643, 821)
                        sleep(1000)
                    }
                    clickA(499, 319)
                    sleep(1000)
                    clickA(1093, 644)
                    sleep(1000)
                    reset()
                } else {
                    sleep(1000 * 60 * 10)
                    for (a in 0..10) {
                        clickA(643, 821)
                        sleep(1000)
                    }
                    clickA(499, 319)
                    sleep(1000)
                    clickA(1093, 644)
                    sleep(1000)
                    reset()
                }
            }
        }
    }

    private fun huodong9(hour: Int, minute: Int, ocrBlock: ArrayList<TextBlock>) {
        if (hour == 21 && minute == 30) {
            runStep = 0
            isHuodong = false
        }
        //1 5
        if (TimeUtils.isTimeA()) {
            if (hour == 21 && minute in 0..20) {
                if (ocrBlock.find { it.text == "点击前往" } != null && runStep == 0) {
                    runStep = 1
                    val node = ocrBlock.find { it.text == "点击前往" }
                    val maxX = node!!.boxPoint.maxBy { it.x }
                    val maxY = node.boxPoint.maxBy { it.y }
                    val minX = node.boxPoint.minBy { it.x }
                    val minY = node.boxPoint.minBy { it.y }
                    clickA(minX.x, minY.y, maxX.x, maxY.y)
                    sleep(4000)
                } else if (runStep == 0) {
                    runStep = 1
                    clickA(1255, 84)
                    sleep(4000)
                }
                if (ocrBlock.find { it.text == "前往" } != null && runStep == 1) {
                    runStep = 2
                    val node = ocrBlock.find { it.text == "前往" }
                    val maxX = node!!.boxPoint.maxBy { it.x }
                    val maxY = node!!.boxPoint.maxBy { it.y }
                    val minX = node!!.boxPoint.minBy { it.x }
                    val minY = node!!.boxPoint.minBy { it.y }
                    clickA(minX.x, minY.y, maxX.x, maxY.y)
                    sleep(4000)
                    isHuodong = true
                }
                if (runStep == 2) {
                    val node = ocrBlock.find {
                        it.text.contains("点我") || it.text.contains("我进") || it.text.contains("战场")
                    }
                    if (node != null) {
                        val maxX = node!!.boxPoint.maxBy { it.x }
                        val maxY = node!!.boxPoint.maxBy { it.y }
                        val minX = node!!.boxPoint.minBy { it.x }
                        val minY = node!!.boxPoint.minBy { it.y }
                        clickA((maxX.x + minX.x) / 2, (maxY.y + (maxY.y - minY.y)))
                        sleep(1000)
                        runStep = 3
                    }
                }
                if (runStep == 3) {
                    if (ClickXY.click4Str("进入", 4000, ocrBlock)) {
                        runStep = 4
                    } else {
                        runStep = 2
                    }
                }
                if (runStep == 4) {
                    val node = ocrBlock.find {
                        it.text.contains("点我") || it.text.contains("我进") || it.text.contains("战场")
                    }
                    if (node != null) {
                        runStep = 2
                    } else {
                        if (ocrBlock.find { it.text.contains("自动") } == null) {
                            clickA(1731, 469)
                        }
                    }
                }
            }

        } else if (TimeUtils.isTimeB()) {
            if (hour == 21 && minute in 0..15) {
                if (ocrBlock.find { it.text == "点击前往" } != null && runStep == 0) {
                    runStep = 1
                    val node = ocrBlock.find { it.text == "点击前往" }
                    val maxX = node!!.boxPoint.maxBy { it.x }
                    val maxY = node.boxPoint.maxBy { it.y }
                    val minX = node.boxPoint.minBy { it.x }
                    val minY = node.boxPoint.minBy { it.y }
                    clickA(minX.x, minY.y, maxX.x, maxY.y)
                    sleep(4000)
                } else if (runStep == 0) {
                    runStep = 1
                    clickA(1255, 84)
                    sleep(4000)
                }
                if (ocrBlock.find { it.text == "前往" } != null && runStep == 1) {
                    runStep = 2
                    val node = ocrBlock.find { it.text == "前往" }
                    val maxX = node!!.boxPoint.maxBy { it.x }
                    val maxY = node!!.boxPoint.maxBy { it.y }
                    val minX = node!!.boxPoint.minBy { it.x }
                    val minY = node!!.boxPoint.minBy { it.y }
                    clickA(minX.x, minY.y, maxX.x, maxY.y)
                    sleep(3000)
                }

                if (runStep == 2) {
                    if (ClickXY.click4Str("前往", 6000, ocrBlock)) {
                        runStep = 3
                        isHuodong = true
                    }
                }
                //挂机
                if (runStep == 3) {
                    if (ocrBlock.find { it.text.contains("自动") } == null) {
                        if (retryTimes == 0) {
                            ClickXY.autoPlay(1000 * 30)
                            retryTimes = 5
                        } else {
                            retryTimes--
                        }
                    } else {
                        sleep(1000 * 30)
                    }
                }
            }
        } else {
            if (hour == 21 && minute in 0..30) {
                clickA(1134, 107)
                sleep(1000)
                clickA(1301, 822)
                sleep(5000)
                isGs = true
            }

        }

    }

    private fun kf(hour: Int, minute: Int, ocrBlock: ArrayList<TextBlock>) {
        if (hour == 0 && minute == 1) {
            kfBossTimes = 20
            mjTimes = 5
        }
    }

    private fun mh(hour: Int, minute: Int, ocrBlock: ArrayList<TextBlock>) {
        if (mhTime == 0L || (System.currentTimeMillis() - mhTime) > 1000 * 60 * 3) {
            if (!isInHuodongTime(hour, minute) ) {
                if (runStep == 0 && minute < 53) {
                    clickA(1399, 233)
                    sleep(1500)
                    clickA(1066, 371)
                    sleep(1500)
                    if (mhType == 2) {
                        clickA(441, 607)
                    }
                 /*   if (mhType == 1) {
                        clickA(418, 392)
                    } else if (mhType == 2) {
                        clickA(441, 607)
                    } else if (mhType == 3) {
                        clickA(467, 782)
                    }*/
                    sleep(1500)
                    runStep = 1
                } else if (runStep == 1 && minute < 54) {
                    val node = ocrBlock.find { it.text.contains("本层剩余") }
                    if (node != null) {
                        if (node.text.contains("1") || node.text.contains("2") || node.text.contains(
                                "3"
                            ) || node.text.contains("4") || node.text.contains("5")
                        ) {
                            clickA(935, 873)
                            sleep(5000)
                            ClickXY.openMap(1000)
                            ClickXY.autoPlay(1000)
                            isMhMap = true
                            isHuodong = true
                            runStep = 2
                        } else {
                            if (retryTimes > 0) {
                                retryTimes--
                            } else {
                                retryTimes = 5
                                clickA(1629, 154)
                                sleep(1000)
                                runStep = 0
                                if (mhType == 1) {
                                    mhType = 2
                                } /*else if (mhType == 2) {
                                    mhType = 3
                                }*/ else {
                                    mhType = 1
                                    mhTime = System.currentTimeMillis()
                                }
                            }

                        }
                    } else {
                        if (retryTimes > 0) {
                            retryTimes--
                            sleep(1000)
                        } else {
                            retryTimes = 5
                            runStep = 0
                        }
                    }
                }
            }
        } else {
            //禁地
            jd(hour, minute, ocrBlock)
        }
    }

    private fun jd(hour: Int, minute: Int, ocrBlock: ArrayList<TextBlock>) {
        if (!isInHuodongTime(hour, minute) && !isHuodong && !isMhMap ) {
            if (jdBossTime == 0L || (System.currentTimeMillis() - jdBossTime) > 1000 * 60 * 60 * 2) {
                ClickXY.openBossMap(1000)
                clickA(1227, 628)
                sleep(1000)
                clickA(1072, 320)
                sleep(3000)
                for (i in 0..5) {
                    clickA(1446, 899)
                    sleep(3000)
                }
                clickA(719, 276)
                sleep(3000)
                for (i in 0..5) {
                    clickA(1446, 899)
                    sleep(3000)
                }
                clickA(344, 320)
                sleep(3000)
                for (i in 0..5) {
                    clickA(1446, 899)
                    sleep(3000)
                }
                swipe(400, 320, 700, 320)
                sleep(2000)
                swipe(422, 320, 722, 320)
                sleep(4000)
                clickA(344, 320)
                sleep(3000)
                for (i in 0..5) {
                    clickA(1446, 899)
                    sleep(3000)
                }
                jdBossTime = System.currentTimeMillis()
                if (enterJdTime == 0L || (System.currentTimeMillis() - enterJdTime) > 1000 * 60 * 60 * 5) {
                    swipe(700, 320, 400, 320)
                    sleep(2000)
                    swipe(722, 320, 422, 320)
                    sleep(4000)
                    clickA(1479, 269)
                    sleep(3000)
                    clickA(1446, 899)
                    sleep(1000 * 60 * 3)
                    enterJdTime = System.currentTimeMillis()
                } else {
                    clickA(1629, 154)
                    sleep(3000)
                }
            } else {
                mj(hour, minute, ocrBlock)
            }
        }
    }

    private fun isInHuodongTime(hour: Int, minute: Int): Boolean {
        return if (hour == 12 && minute in 0..11) {
            true
        } else if (hour == 19 && minute in 30..42) {
            true
        } else if (hour == 20 && minute in 0..15) {
            true
        } else if ((minute in 55..59 || minute in 0..5) && kfBossTimes > 0) {
            true
        } else hour == 21 && minute in 0..30
    }

    private fun hasStr(
        strs: MutableList<String>,
        x: Int,
        y: Int,
        ocrBlock: ArrayList<TextBlock>
    ): TextBlock? {
        val node = ocrBlock.find {
            it.text.contains(strs[0]) || it.text.contains(strs[1]) || it.text.contains(strs[2])
        }
        if (node != null) {
            val maxX = node.boxPoint.maxBy { it.x }
            val maxY = node.boxPoint.maxBy { it.y }
            val minX = node.boxPoint.minBy { it.x }
            val minY = node.boxPoint.minBy { it.y }
            if (x in minX.x..maxX.x && y in minY.y..maxY.y) {
                return node
            }
        }
        return null
    }


    private fun goBoss() {
        clickA(1377, 216)
        sleep(4000)
        clickA(855, 722)
        sleep(1000)
        clickA(385, 547)
        sleep(1000)
        clickA(1107, 866)
        sleep(5000)
        clickA(1096, 641)
        sleep(5000)
        ClickXY.autoPlay(1000 * 60)
    }

    private fun mj(hour: Int, minute: Int, ocrBlock: ArrayList<TextBlock>) {
        if (!isInHuodongTime(hour, minute) && mjTimes > 0 ) {
            if (minute < 50 && !isMjMap && !isHuodong) {
                if (mjBossTime == 0L || (System.currentTimeMillis() - mjBossTime) > 1000 * 60 * 5) {
                    if (runStep == 0) {
                        ClickXY.openBossMap(1000)
                        clickA(1588, 703)
                        sleep(1000)
                        runStep = 1
                    } else if (runStep == 1) {
                        val nodes = ocrBlock.filter { it.text.contains("数量") }
                        if (nodes.isNotEmpty() && nodes.size == 2) {
                            if (!nodes[0].text.contains("0") || !nodes[1].text.contains("0")) {
                                clickA(1538, 870)
                                sleep(4000)
                                isDie = false
                                isMjMap = true

                            } else {
                                ClickXY.closeBossMap(1000)
                                mjBossTime = System.currentTimeMillis()
                            }
                        }

                    }
                }
            }
        }
    }

    /**
     * 初始化配置
     */
    private fun reset() {
        isMhMap = false
        isHuodong = false
        isMjMap = false
        isDie = false
        isGs = false
        lastRunTime = 0L
        retryTimes = 5
        runStep = 0
    }

    private fun login(ocrBlock: ArrayList<TextBlock>) {
        if (ocrBlock.find { it.text.contains("影魅") } != null) {
            clickA(966, 822)
            sleep(3000)
            clickA(950, 152)
            sleep(3000)
            reLineTime = 0L
            return
        }
        if (ocrBlock.find { it.text.contains("视频问题") } != null) {
            ClickXY.click4Str("确定", 2000, ocrBlock)
            return
        }
        if (ocrBlock.find { it.text == "游戏声明" } != null) {
            clickA(1641, 150)
            sleep(2000)
            return
        }
        if (ClickXY.click4Str("健康游戏", 30000, ocrBlock)) {
            clickA(966, 837)
            sleep(2000)
            reset()
            return
        }

        if (ClickXY.click4Str("开始游戏", 6000, ocrBlock)) {
            return
        }
        if (ocrBlock.find { it.text.contains("断开连接") || it.text.contains("连接服务器失败") } != null) {
            if (ClickXY.click4StrEqual("确定", 1000, ocrBlock)) {
                return
            }
        }
        if (ocrBlock.find { it.text.contains("强行登录") } != null) {
            if (ClickXY.click4StrEqual("确定", 1000, ocrBlock)) {
                return
            }
        }
        if (ocrBlock.find { it.text.contains("重连中") } != null) {
            if (reLineTime == 0L) {
                reLineTime = System.currentTimeMillis()
            } else if ((System.currentTimeMillis() - reLineTime) > 1000 * 10) {
                MyApp.runRuleApp?.let { AppUtils.closeApp(MyApp.instance, it) }
                sleep(5000)
                MyApp.runRuleApp?.let { AppUtils.openApp(MyApp.instance, it) }
            }
            return
        }
    }

}